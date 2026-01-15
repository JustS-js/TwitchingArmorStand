package justs_js.twas.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mrbysco.armorposer.Reference;
import com.mrbysco.armorposer.data.SyncData;
import com.mrbysco.armorposer.packets.ArmorStandSyncPayload;
import justs_js.cel.CELModLib;
import justs_js.cel.client.api.ClientBrain;
import justs_js.cel.client.api.ClientEntity;
import justs_js.cel.client.api.behaviour.*;
import justs_js.twas.TWASManager;
import justs_js.twas.TWASModClient;
import justs_js.twas.config.TWASConfig;
import justs_js.twas.entity.behavior.CustomClientFollowTargetSink;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TwitchingArmorStand extends ClientEntity {
    public TwitchingArmorStand(EntityType<? extends ClientEntity> entityType, Level level) {
        super(entityType, level);
    }

    private String boundedNickname;
    private UUID boundedUUID;

    private final TwitchingAnimator animator = new TwitchingAnimator(1f);

    @Override
    public boolean isPushable() {
        return super.isPushable() && TWASModClient.CONFIG.entitiesSettings.entityCollision;
    }

    @Override
    protected void pushEntities() {
        if (!TWASModClient.CONFIG.entitiesSettings.entityCollision) return;
        TWASManager.forEach(
                (entity) -> {
                    if (this.getBoundingBox().intersects(entity.getBoundingBox())) {
                        this.doPush(entity);
                    }
                }
        );
        LocalPlayer player = Minecraft.getInstance().player;
        if (this.getBoundingBox().intersects(player.getBoundingBox())) {
            this.doPush(player);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getBoundedNickname() == null || getBoundedNickname().isEmpty() || getBoundedUUID() == null || getBoundedArmorStand() == null) {
            return;
        }
        animator.tick(position(), oldPosition());
        sendArmorPoserPacket(createArmorStandCompound(true));
    }

    public void requestJump() {
        Minecraft.getInstance().execute(
                () -> {
                    ClientBrain<TwitchingArmorStand> brain = (ClientBrain<TwitchingArmorStand>)this.getBrain();
                    brain.stopAll((ClientLevel) this.level(), this);
                    brain.setMemory(MemoryModuleType.WALK_TARGET, Optional.empty());
                    brain.setActiveActivityIfPossible(Activity.CELEBRATE);
                }
        );
    }

    public void requestFollow(String entityName) {
        Minecraft.getInstance().execute(
                () -> {
                    List<Entity> entitiesWithRequestedName = this.level().getEntities(
                            this,
                            this.getBoundingBox().inflate(64),
                            (e) -> {
                                String textName = e.getPlainTextName().toLowerCase();
                                return textName.startsWith(entityName);
                            }
                    );
                    if (entitiesWithRequestedName.isEmpty()) return;
                    Entity first = entitiesWithRequestedName.stream().filter(
                            (e) -> !e.getUUID().equals(this.getBoundedUUID())
                    ).findFirst().orElse(null);
                    ClientBrain<TwitchingArmorStand> brain = (ClientBrain<TwitchingArmorStand>)this.getBrain();
                    brain.stopAll((ClientLevel) this.level(), this);
                    this.setFollowTargetEntity(first);
                    brain.setActiveActivityIfPossible(Activity.INVESTIGATE);
                    TWASModClient.CONFIG.twitchNameToSerialized.get(getBoundedNickname()).followTarget =
                            (first != null) ? first.getUUID() : null;
                    AutoConfig.getConfigHolder(TWASConfig.class).save();
                }
        );
    }

    @Override
    public void checkDespawn() {
        super.checkDespawn();
        if (this.getBoundedArmorStand() == null) {
            TWASManager.removeTickableEntity(this);
            this.discard();
        }
    }

    public void emote(String emote) {
        switch (emote) {
            case "twerk" -> this.animator.setAnimateTwerk(true);
            case "hello" -> this.animator.setAnimateHello(true);
            case "clap" -> this.animator.setAnimateClap(true);
            case null, default -> {return;}
        }
    }

    protected void registerBrainGoals(ClientBrain<? extends ClientEntity> brain) {
        brain.setSchedule(Schedule.EMPTY);
        brain.addActivity(Activity.IDLE, ImmutableList.of(
                Pair.of(0, new ClientRunOne(
                        ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED, MemoryModuleType.GAZE_COOLDOWN_TICKS, MemoryStatus.REGISTERED),
                        ImmutableList.of(
                                Pair.of(new ClientDoNothing(30, 60), 1),
                                Pair.of(ClientSetEntityLookTarget.create((e) -> !e.getUUID().equals(this.getBoundedUUID()), 4.0F), 1),
                                Pair.of(new ClientRandomLookAround(BiasedToBottomInt.of(100, 200), 60.0F, 30.0F, 90.0F), 1),
                                Pair.of(new ClientLookAtTargetSink(30, 60), 1)
                        ))
                ),
                Pair.of(2, new ClientRunOne(
                        ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED),
                        ImmutableList.of(
                                Pair.of(new ClientDoNothing(60, 120), 1),
                                Pair.of(ClientRandomStroll.stroll(0.75F), 1),
                                Pair.of(new ClientJumpOnSpot(), 1),
                                Pair.of(new ClientMoveToTargetSink(), 1)
                        ))
                ))
        );
        brain.addActivity(Activity.CORE, ImmutableList.of(Pair.of(0, new ClientSwim(0.3F))));
        brain.addActivity(Activity.CELEBRATE, ImmutableList.of(Pair.of(0, new ClientJumpOnSpot())));
        brain.addActivity(Activity.INVESTIGATE, ImmutableList.of(
                Pair.of(0, new CustomClientFollowTargetSink(0.9F, 4)),
                Pair.of(0, new ClientLookAtTargetSink(30, 60)),
                Pair.of(0, new ClientMoveToTargetSink())
        ));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
    }

    private ArmorStand getBoundedArmorStand() {
        return (ArmorStand) this.level().getEntity(getBoundedUUID());
    }

    public CompoundTag createArmorStandCompound(boolean shouldSync) {
        CompoundTag original;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(Reference.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(
                    problemreporter$scopedcollector,
                    this.getBoundedArmorStand().registryAccess()
            );
            this.getBoundedArmorStand().saveWithoutId(output);
            original = output.buildResult();
        }
        CompoundTag compoundTag = new CompoundTag();

        CompoundTag poseTag = new CompoundTag();
        ListTag poseHeadTag = new ListTag();
        poseHeadTag.add(FloatTag.valueOf(this.getXRot()));
        poseHeadTag.add(FloatTag.valueOf(this.yHeadRot - this.yBodyRot));
        float headTilt = 0;
        if (original.contains("Pose")) {
            CompoundTag originalPoseTag = original.getCompoundOrEmpty("Pose");
            if (originalPoseTag.contains("Head")) {
                ListTag originalHeadTag = originalPoseTag.getListOrEmpty("Head");
                if (originalHeadTag.size() > 2) {
                    headTilt = originalHeadTag.getFloatOr(2, 0f);
                }
            }
        }
        poseHeadTag.add(FloatTag.valueOf(headTilt));
        poseTag.put("Head", poseHeadTag);
        if (this.animator.getAnimateMovement()) {
            poseTag.merge(this.animator.getAnimatedPoseState());
        }
        compoundTag.put("Pose", poseTag);

        ListTag rotationTag = new ListTag();
        rotationTag.add(FloatTag.valueOf(this.yBodyRot));
        rotationTag.add(FloatTag.valueOf(0));
        compoundTag.put("Rotation", rotationTag);

        ListTag positionOffset = new ListTag();

        if (this.tickCount % 4 == 0) {
            Vec3 armorStandPos = this.getBoundedArmorStand().getPosition(1);
            double dx = this.getX() - this.xOld;
            double dy = this.getY() - this.yOld;
            double dz = this.getZ() - this.zOld;
            double threshold = 1E-2d;
            if (shouldSync) {
                dx = this.getX() - armorStandPos.x;
                dy = this.getY() - armorStandPos.y;
                dz = this.getZ() - armorStandPos.z;
            }
            dx /= 2;
            dy /= 2;
            dz /= 2;
            positionOffset.add(DoubleTag.valueOf((dx < -(threshold) || dx > threshold)?dx:0));
            positionOffset.add(DoubleTag.valueOf((dy < -(threshold) || dy > threshold)?dy:0));
            positionOffset.add(DoubleTag.valueOf((dz < -(threshold) || dz > threshold)?dz:0));
            compoundTag.put("Move", positionOffset);
        }

        compoundTag.putBoolean("NoGravity", true);

        CompoundTag toBeMergedWithCompoundTag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(Reference.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(problemreporter$scopedcollector, this.getBoundedArmorStand().registryAccess());
            this.getBoundedArmorStand().saveWithoutId(output);
            toBeMergedWithCompoundTag = output.buildResult();
        }
        return toBeMergedWithCompoundTag.merge(compoundTag);
    }

    public void sendArmorPoserPacket(CompoundTag compoundTag) {
        SyncData data = new SyncData(
                this.getBoundedUUID(),
                compoundTag
        );
        ClientPlayNetworking.send(new ArmorStandSyncPayload(data));
    }

    public void bound(String nickname) {
        this.boundedNickname = nickname;
    }

    public void bound(UUID uuid) {
        this.boundedUUID = uuid;
    }

    public String getBoundedNickname() {
        return boundedNickname;
    }

    public UUID getBoundedUUID() {
        return boundedUUID;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.JUMP_STRENGTH, 0.7);
    }
}
