package justs_js.twas.entity.behavior;

import justs_js.cel.client.api.ClientEntity;
import justs_js.cel.client.api.behaviour.ClientFollowTargetSink;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

public class CustomClientFollowTargetSink extends ClientFollowTargetSink {
    private final double playerThreshold;

    public CustomClientFollowTargetSink(float f, double threshold) {
        super(f);
        this.playerThreshold = threshold;
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, ClientEntity livingEntity) {
        Entity entity = livingEntity.getFollowTargetEntity();
        return livingEntity.isAlive() && entity != null &&
                (!(entity instanceof AbstractClientPlayer) || livingEntity.distanceToSqr(entity) > playerThreshold*playerThreshold);
    }
}
