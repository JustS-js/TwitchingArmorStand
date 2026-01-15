package justs_js.twas.config;

import justs_js.twas.TWASModClient;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Config(name = TWASModClient.MOD_ID)
public class TWASConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public Map<String, SerializedEntity> twitchNameToSerialized = new HashMap<>();

    @ConfigEntry.Gui.CollapsibleObject
    public TwitchIntegration twitchIntegration = new TwitchIntegration();

    public static class TwitchIntegration {
        public String clientId = "";
        public String clientSecret = "";
        public String accessToken = "";
        public String refreshToken = "";
        public String userId = "";
    }

    public static class SerializedEntity {
        public UUID boundedArmorStand;
        public String lastCommand;
        public UUID followTarget;
    }
}
