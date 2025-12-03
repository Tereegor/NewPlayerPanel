package newplayerpanel.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class ActionBarUtil {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static Method sendActionBarMethod;
    private static Method sendMessageMethod;
    private static boolean methodsInitialized = false;
    
    static {
        initializeMethods();
    }
    
    private static void initializeMethods() {
        if (methodsInitialized) {
            return;
        }
        
        try {
            Class<?> playerClass = Player.class;
            
            for (Method method : playerClass.getMethods()) {
                if (method.getName().equals("sendActionBar") && 
                    method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    String paramTypeName = paramType.getName();
                    if (paramTypeName.equals("net.kyori.adventure.text.Component") || 
                        paramTypeName.startsWith("net.kyori.adventure")) {
                        sendActionBarMethod = method;
                        break;
                    }
                }
            }
            
            for (Method method : playerClass.getMethods()) {
                if (method.getName().equals("sendMessage") && 
                    method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    String paramTypeName = paramType.getName();
                    if (paramTypeName.equals("net.kyori.adventure.text.Component") || 
                        paramTypeName.startsWith("net.kyori.adventure")) {
                        sendMessageMethod = method;
                        break;
                    }
                }
            }
            
            methodsInitialized = true;
        } catch (Exception e) {
        }
    }
    
    public static void sendActionBar(Player player, String message) {
        Component component = convertLegacyToComponent(message);
        sendActionBar(player, component);
    }
    
    public static void sendActionBar(Player player, Component component) {
        try {
            String legacyMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
            
            if (sendActionBarMethod != null) {
                try {
                    Class<?> serverMiniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
                    Object serverMiniMessage = serverMiniMessageClass.getMethod("miniMessage").invoke(null);
                    Object serverComponent = serverMiniMessageClass.getMethod("deserialize", String.class).invoke(serverMiniMessage, legacyMessage);
                    sendActionBarMethod.invoke(player, serverComponent);
                    return;
                } catch (Exception e) {
                }
            }
            
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(legacyMessage));
        } catch (Exception e) {
            try {
                String legacyMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
                player.sendMessage(legacyMessage);
            } catch (Exception ex) {
            }
        }
    }
    
    public static void sendMessage(Player player, String message) {
        Component component = convertLegacyToComponent(message);
        sendMessage(player, component);
    }
    
    public static void sendMessage(Player player, Component component) {
        try {
            String legacyMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
            
            if (sendMessageMethod != null) {
                try {
                    Class<?> serverMiniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
                    Object serverMiniMessage = serverMiniMessageClass.getMethod("miniMessage").invoke(null);
                    Object serverComponent = serverMiniMessageClass.getMethod("deserialize", String.class).invoke(serverMiniMessage, legacyMessage);
                    sendMessageMethod.invoke(player, serverComponent);
                    return;
                } catch (Exception e) {
                }
            }
            
            player.sendMessage(legacyMessage);
        } catch (Exception e) {
        }
    }
    
    private static Component convertLegacyToComponent(String message) {
        String miniMessage = convertLegacyColors(message);
        return MINI_MESSAGE.deserialize(miniMessage);
    }
    
    private static String convertLegacyColors(String message) {
        message = message.replace("§0", "&0")
                .replace("§1", "&1")
                .replace("§2", "&2")
                .replace("§3", "&3")
                .replace("§4", "&4")
                .replace("§5", "&5")
                .replace("§6", "&6")
                .replace("§7", "&7")
                .replace("§8", "&8")
                .replace("§9", "&9")
                .replace("§a", "&a")
                .replace("§b", "&b")
                .replace("§c", "&c")
                .replace("§d", "&d")
                .replace("§e", "&e")
                .replace("§f", "&f")
                .replace("§l", "&l")
                .replace("§m", "&m")
                .replace("§n", "&n")
                .replace("§o", "&o")
                .replace("§k", "&k")
                        .replace("§r", "&r");
        
        return message.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underline>")
                .replace("&o", "<italic>")
                .replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }
}
