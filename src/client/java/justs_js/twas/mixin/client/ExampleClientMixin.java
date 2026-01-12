package justs_js.twas.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import justs_js.integrationapi.impl.twitch.eventsub.TwitchIntegrationImpl;
import justs_js.twas.TWASModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.http.HttpResponse;

@Mixin(value = TwitchIntegrationImpl.class, remap = false)
public class ExampleClientMixin {
	@Inject(
			method = "refreshTokens",
			at = @At(
					value = "INVOKE",
					target = "Ljava/net/http/HttpResponse;statusCode()I"
			)
	)
	private void init(CallbackInfo info, @Local HttpResponse<String> response) {
		//TWASModClient.LOGGER.info(response.request().toString());
	}
}