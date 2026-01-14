package justs_js.twas.mixin.client;

import justs_js.cel.client.api.behaviour.ClientBehavior;
import justs_js.twas.TWASModClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientBehavior.class, remap = false)
public abstract class ExampleClientMixin<E extends LivingEntity> {
	@Shadow public abstract String debugString();

	@Inject(method = "hasRequiredMemories", at = @At("RETURN"), cancellable = true)
	private void hasRequiredMemories(E livingEntity, CallbackInfoReturnable<Boolean> cir) {
		TWASModClient.LOGGER.info("hasRequiredMemories for {}: {}", this.debugString(), cir.getReturnValueZ());
	}

	@Redirect(method = "hasRequiredMemories", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;checkMemory(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Lnet/minecraft/world/entity/ai/memory/MemoryStatus;)Z"))
	private boolean hasRequiredMemories(Brain instance, MemoryModuleType<?> memoryModuleType, MemoryStatus memoryStatus) {
		boolean a = instance.checkMemory(memoryModuleType, memoryStatus);
		TWASModClient.LOGGER.info("checkMemory for {}: {} | {} | {}", this.debugString(), memoryModuleType, memoryStatus, a);
		return a;
	}

	@Inject(method = "tryStart(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;J)Z", at = @At("RETURN"), cancellable = true)
	private void tryStart(ClientLevel clientLevel, E livingEntity, long l, CallbackInfoReturnable<Boolean> cir) {
		TWASModClient.LOGGER.info("tryStart for {}: {}", this.debugString(), cir.getReturnValueZ());
	}

	@Inject(method = "timedOut", at = @At("RETURN"), cancellable = true)
	private void timedOut(long l, CallbackInfoReturnable<Boolean> cir) {
		TWASModClient.LOGGER.info("timedOut for {}: {}", this.debugString(), cir.getReturnValueZ());
	}

	@Inject(method = "checkExtraStartConditions", at = @At("RETURN"), cancellable = true)
	private void checkExtraStartConditions(ClientLevel clientLevel, E livingEntity, CallbackInfoReturnable<Boolean> cir) {
		TWASModClient.LOGGER.info("checkExtraStartConditions for {}: {}", this.debugString(), cir.getReturnValueZ());
	}

	@Inject(method = "start", at = @At("RETURN"))
	private void start(ClientLevel clientLevel, E livingEntity, long l, CallbackInfo ci) {
		TWASModClient.LOGGER.info("start for {}", this.debugString());
	}

	@Inject(method = "tickOrStop(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;J)V", at = @At(value = "INVOKE", target = "Ljusts_js/cel/client/api/behaviour/ClientBehavior;tick(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;J)V"))
	private void tick(ClientLevel clientLevel, E livingEntity, long l, CallbackInfo ci) {
		TWASModClient.LOGGER.info("tick for {}", this.debugString());
	}

	@Inject(method = "tickOrStop(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;J)V", at = @At(value = "INVOKE", target = "Ljusts_js/cel/client/api/behaviour/ClientBehavior;doStop(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;J)V"))
	private void doStop(ClientLevel clientLevel, E livingEntity, long l, CallbackInfo ci) {
		TWASModClient.LOGGER.info("doStop for {}", this.debugString());
	}
}