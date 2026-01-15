package justs_js.twas;

import com.mojang.brigadier.arguments.StringArgumentType;
import justs_js.cel.CELModLib;
import justs_js.integrationapi.api.ApiConfig;
import justs_js.integrationapi.impl.twitch.eventsub.TwitchEventTypes;
import justs_js.integrationapi.impl.twitch.eventsub.TwitchIntegrationImpl;
import justs_js.integrationapi.impl.twitch.eventsub.event.TwitchChatMessageEvent;
import justs_js.integrationapi.impl.twitch.eventsub.event.TwitchIntegrationRefreshTokenEvent;
import justs_js.twas.config.TWASConfig;
import justs_js.twas.entity.TwitchingArmorStand;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class TWASModClient implements ClientModInitializer {
	public static final String MOD_ID = "twas";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static TWASConfig CONFIG;
	private static TwitchIntegrationImpl twitch;

	public static EntityType<TwitchingArmorStand> TWITCHING_ARMOR_STAND = CELModLib.controller.register(
			"twitching_armor_stand",
			EntityType.Builder
					.of(TwitchingArmorStand::new, MobCategory.MISC)
					.sized(0.49F, 1.97F)
					.eyeHeight(1.7775F)
	);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Twitching Armor Stand comes to the party!");
		AutoConfig.register(TWASConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(TWASConfig.class).getConfig();

		initClientEvents();
		initTwitchIntegration();
		initClientEntities();
	}

	private void initTwitchIntegration() {
		ApiConfig.Builder configBuilder = new ApiConfig.Builder();
		twitch = new TwitchIntegrationImpl(
				configBuilder
						.enableEvent(TwitchEventTypes.getInstance().INTEGRATION_REFRESH_TOKEN)
						.enableEvent(TwitchEventTypes.getInstance().CHANNEL_CHAT_MESSAGE)
						.withApiName("Twitch-API")
						.withAuthParam("clientId", CONFIG.twitchIntegration.clientId)
						.withAuthParam("clientSecret", CONFIG.twitchIntegration.clientSecret)
						.withAuthParam("accessToken", CONFIG.twitchIntegration.accessToken)
						.withAuthParam("refreshToken", CONFIG.twitchIntegration.refreshToken)
						.withApiParam(
								"channel.chat.message",
								"{\"broadcaster_user_id\": \"" + CONFIG.twitchIntegration.userId + "\", \"user_id\": \"" + CONFIG.twitchIntegration.userId + "\"}"
						)
						.withErrorHandler(
								(ex, context) -> {
									LOGGER.error("{}", ex.toString());
									if (Minecraft.getInstance().player != null) {
										Minecraft.getInstance().execute(
												() -> Minecraft.getInstance().player.displayClientMessage(
														Component.literal("§cTwitch error: " + ex), false
												)
										);
									}
								}
						)
						.build()
		);
		twitch.subscribe(
				TwitchEventTypes.getInstance().CHANNEL_CHAT_MESSAGE,
				(event) -> {
					String nickname = ((TwitchChatMessageEvent) event).getChatterUserLogin();
					String command = ((TwitchChatMessageEvent) event).getMessageText();
					if (CONFIG.twitchNameToSerialized.containsKey(nickname) && command.startsWith("!s ")) {
						LOGGER.info("[{}]: {}",nickname,command);
						TWASManager.applyCommand(nickname,command.toLowerCase(Locale.ROOT));
					}
				}
		);
		twitch.subscribe(
				TwitchEventTypes.getInstance().INTEGRATION_REFRESH_TOKEN,
				event -> {
					CONFIG.twitchIntegration.accessToken = ((TwitchIntegrationRefreshTokenEvent) event).getAccessToken();
					CONFIG.twitchIntegration.refreshToken = ((TwitchIntegrationRefreshTokenEvent) event).getRefreshToken();
					AutoConfig.getConfigHolder(TWASConfig.class).save();
				}
		);
	}

	private void initClientEvents() {
		ClientPlayConnectionEvents.JOIN.register(
				(handler, sender, client) -> {
					Thread t = new Thread(() -> twitch.start());
					t.setDaemon(true);
					t.start();
				}
		);
		ClientEntityEvents.ENTITY_LOAD.register(
				(entity, world) -> {
					if (entity instanceof ArmorStand) TWASManager.syncByUUID(entity.getUUID());
				}
		);

		ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> {
					Thread t = new Thread(() -> twitch.stop());
					t.setDaemon(true);
					t.start();
				}
		);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommandManager.literal("twas")
							.then(ClientCommandManager.literal("bound")
									.then(ClientCommandManager.argument("nickname", StringArgumentType.word())
											.executes((commandContext) -> {
												Entity entity = Minecraft.getInstance().crosshairPickEntity;
												if (entity == null) return 0;
												if (!(entity instanceof ArmorStand)) return 0;
												String nickname = commandContext.getArgument(
														"nickname",
														String.class
												).toLowerCase(Locale.ROOT);
												TWASConfig.SerializedEntity serialized = new TWASConfig.SerializedEntity();
												serialized.boundedArmorStand = entity.getUUID();
												serialized.lastCommand = "stop";
												serialized.followTarget = null;
												CONFIG.twitchNameToSerialized.put(
														nickname,
														serialized
                                                );
												AutoConfig.getConfigHolder(TWASConfig.class).save();
												TWASManager.syncByNickname(nickname);
												commandContext.getSource().sendFeedback(
														Component.literal("bounded with " + nickname)
												);
												return 1;
											}
									)
							)
					)
							.then(ClientCommandManager.literal("unbound")
									.executes((commandContext) -> {
												Entity entity = Minecraft.getInstance().crosshairPickEntity;
												if (entity == null) return 0;
												if (!(entity instanceof ArmorStand)) return 0;
												AtomicReference<TwitchingArmorStand> atomicStandEntity = new AtomicReference<>();
												CELModLib.controller.forEach(
														(e) -> {
															TwitchingArmorStand stand = (TwitchingArmorStand)e;
															if (stand.getBoundedUUID().equals(entity.getUUID())){
																atomicStandEntity.set(stand);
															}
														}
												);
												TwitchingArmorStand standEntity = atomicStandEntity.get();
												if (standEntity == null) {
													commandContext.getSource().sendFeedback(Component.literal("no bonds found"));
													return 0;
												}
												String nickname = standEntity.getBoundedNickname();
												CONFIG.twitchNameToSerialized.remove(nickname);
												TWASManager.syncByNickname(nickname);
												commandContext.getSource().sendFeedback(Component.literal("unbounded with " + nickname));
												return 1;
											}
									)
									.then(ClientCommandManager.argument("nickname", StringArgumentType.word())
											.executes((commandContext) -> {
														String nickname = commandContext.getArgument("nickname", String.class);
														CONFIG.twitchNameToSerialized.remove(nickname);
														TWASManager.syncByNickname(nickname);
														commandContext.getSource().sendFeedback(Component.literal("unbounded with " + nickname));
														return 1;
													}
											)
									)
							)
			);
		});
	}

	private void initClientEntities() {
		FabricDefaultAttributeRegistry.register(TWITCHING_ARMOR_STAND, TwitchingArmorStand.createAttributes());
		EntityRenderers.register(TWITCHING_ARMOR_STAND, NoopRenderer::new);
	}
}