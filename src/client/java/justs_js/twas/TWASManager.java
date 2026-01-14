package justs_js.twas;

import justs_js.cel.CELModLib;
import justs_js.cel.client.api.ClientBrain;
import justs_js.twas.entity.TwitchingArmorStand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TWASManager {
    private static final Map<UUID, String> observedUuids = new HashMap<>();

    private static List<String> parseCmd(String cmd) {
        return List.of(cmd.split(" "));
    }

    public static void applyCommand(String nickname, String command) {
        CELModLib.controller.forEach(
                (e) -> {
                    TwitchingArmorStand stand = (TwitchingArmorStand) e;
                    if (!stand.getBoundedNickname().equals(nickname)) return;
                    String cmd = command.substring(3);
                    List<String> parsed = parseCmd(cmd);
                    if (parsed.isEmpty()) return;
                    String first = parsed.getFirst();
                    switch (first) {
                        case "twerk", "hello", "clap" -> stand.emote(first);
                        case "jump" -> stand.requestJump();
                        case "follow" -> stand.requestFollow(parsed.size() > 1 ? parsed.get(1) : "");
                        case "stop" -> Minecraft.getInstance().execute(() -> {
                            stand.setFollowTargetEntity(null);
                            ClientBrain<TwitchingArmorStand> brain = (ClientBrain<TwitchingArmorStand>)stand.getBrain();
                            brain.stopAll((ClientLevel) stand.level(), stand);
                            brain.setActiveActivityIfPossible(Activity.IDLE);
                        });
                        case null, default -> {}
                    }
                }
        );
    }

    public static void sync() {
        for (String nickname : TWASModClient.CONFIG.twitchNameToEntityUUID.keySet()) {
            syncByNickname(nickname);
        }
    }

    public static void syncByUUID(UUID uuid) {
        String nickname = null;
        if (observedUuids.containsKey(uuid)) {
            nickname = observedUuids.get(uuid);
        } else {
            for (Map.Entry<String, UUID> entry : TWASModClient.CONFIG.twitchNameToEntityUUID.entrySet()) {
                if (entry.getValue().equals(uuid)) {
                    nickname = entry.getKey();
                    break;
                }
            }
        }
        if (nickname == null) {
            return;
        }

        if (!uuid.equals(TWASModClient.CONFIG.twitchNameToEntityUUID.get(nickname))) {
            removeAllWith(nickname);
        }
        UUID confUUID = TWASModClient.CONFIG.twitchNameToEntityUUID.get(nickname);
        observedUuids.put(confUUID, nickname);
        createTwitchedArmorStand(nickname, confUUID);
    }

    public static void syncByNickname(String nickname) {
        if (!TWASModClient.CONFIG.twitchNameToEntityUUID.containsKey(nickname)) {
            removeAllWith(nickname);
            return;
        }
        UUID uuid = TWASModClient.CONFIG.twitchNameToEntityUUID.get(nickname);
        if (!observedUuids.containsKey(uuid)) {
            createTwitchedArmorStand(nickname, uuid);
        }
        observedUuids.put(uuid, nickname);
    }

    private static void removeAllWith(String nickname) {
        Set<TwitchingArmorStand> removed = new HashSet<>();
        CELModLib.controller.forEach(
                (e) -> {
                    TwitchingArmorStand stand = (TwitchingArmorStand) e;
                    if (stand.getBoundedNickname().equals(nickname)) {
                        removed.add(stand);
                    }
                }
        );
        for (TwitchingArmorStand stand : removed) {
            observedUuids.remove(stand.getBoundedUUID());
            stand.discard();
        }
    }

    private static void createTwitchedArmorStand(String nickname, UUID uuid) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            TWASModClient.LOGGER.warn("Trying to bond {} with {} without world", uuid, nickname);
            return;
        }
        ArmorStand stand = parseUUID(uuid);
        if (stand == null) {return;}
        TwitchingArmorStand twStand = new TwitchingArmorStand(TWASModClient.TWITCHING_ARMOR_STAND, level);
        Optional<BlockPos> blockPos = level.findSupportingBlock(stand, stand.getBoundingBox().expandTowards(0, 2, 0));
        Vec3 pos = blockPos.orElse(BlockPos.ZERO.atY(stand.getBlockY())).getBottomCenter().add(0, 1, 0);
        twStand.snapTo(new Vec3(stand.getX(), pos.y() + 0.15d, stand.getZ()), stand.getYRot(), stand.getXRot());
        twStand.bound(nickname);
        twStand.bound(uuid);
        CELModLib.controller.addEntity(twStand);
    }

    private static ArmorStand parseUUID(UUID uuid) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            TWASModClient.LOGGER.warn("Trying to parse {} without world", uuid);
            return null;
        }
        Entity entity = level.getEntity(uuid);
        if (entity instanceof ArmorStand stand) {
            return stand;
        }
        TWASModClient.LOGGER.warn("Trying to parse {} that does not belong to Armor Stand", uuid);
        return null;
    }
}
