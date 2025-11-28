package newplayerpanel.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class ActionBarUtil {
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    
    public static void sendActionBar(Player player, String message) {
        Component component = LEGACY_SERIALIZER.deserialize(message);
        player.sendActionBar(component);
    }
}

