package newplayerpanel.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import newplayerpanel.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    
    private final JavaPlugin plugin;
    private final Map<String, String> messages;
    private final MiniMessage miniMessage;
    private String currentLanguage;
    
    public MessageManager(JavaPlugin plugin, StorageProvider storageProvider) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.miniMessage = MiniMessage.miniMessage();
        this.currentLanguage = plugin.getConfig().getString("language", "ru");
    }
    
    public void loadMessages() {
        messages.clear();
        
        java.io.File localizationFolder = new java.io.File(plugin.getDataFolder(), "localization");
        java.io.File languageFile = new java.io.File(localizationFolder, currentLanguage + ".yml");
        
        FileConfiguration langConfig = null;
        
        if (languageFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(languageFile);
            plugin.getLogger().info("Loading localization from file: " + languageFile.getAbsolutePath());
        } else {
            String fileName = "localization/" + currentLanguage + ".yml";
            InputStream stream = plugin.getResource(fileName);
            
            if (stream == null) {
                plugin.getLogger().warning("Could not find localization file: " + fileName + ", falling back to ru.yml");
                stream = plugin.getResource("localization/ru.yml");
            }
            
            if (stream != null) {
                if (!localizationFolder.exists()) {
                    localizationFolder.mkdirs();
                }
                
                try {
                    java.nio.file.Files.copy(stream, languageFile.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Copied default localization to: " + languageFile.getAbsolutePath());
                    
                    langConfig = YamlConfiguration.loadConfiguration(languageFile);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to copy localization file: " + e.getMessage());
                    langConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                }
            }
        }
        
        if (langConfig != null && langConfig.isConfigurationSection("messages")) {
            for (String key : langConfig.getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, langConfig.getString("messages." + key, ""));
            }
        }
        
        plugin.getLogger().info("Loaded " + messages.size() + " messages for language: " + currentLanguage);
    }
    
    public String getRaw(String key) {
        return messages.getOrDefault(key, key);
    }
    
    public Component getComponent(String key) {
        String message = messages.getOrDefault(key, key);
        return miniMessage.deserialize(convertLegacyColors(message));
    }
    
    public Component getComponent(String key, Object... replacements) {
        String message = messages.getOrDefault(key, key);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                message = message.replace("{" + placeholder + "}", value);
            }
        }
        
        return miniMessage.deserialize(convertLegacyColors(message));
    }
    
    public String get(String key) {
        Component component = getComponent(key);
        String result = LegacyComponentSerializer.legacySection().serialize(component);
        return result.replace("\n", " ").replace("\r", "");
    }
    
    public String get(String key, Object... replacements) {
        Component component = getComponent(key, replacements);
        String result = LegacyComponentSerializer.legacySection().serialize(component);
        return result.replace("\n", " ").replace("\r", "");
    }
    
    private String convertLegacyColors(String message) {
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
    
    public Component createClickableCoordsComponent(String world, double x, double y, double z) {
        String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
        String dimension = getDimensionFromWorld(world);
        String command = String.format("/execute in %s run tp @s %.0f %.0f %.0f", dimension, x, y, z);
        String hoverText = getRaw("tracker-coords-hover").replace("{world}", world);
        String text = getRaw("tracker-entry-coords-click").replace("{coords}", coords);
        
        Component textComponent = miniMessage.deserialize(convertLegacyColors(text));
        Component clickableComponent = textComponent.clickEvent(
            net.kyori.adventure.text.event.ClickEvent.runCommand(command));
        Component hoverComponent = miniMessage.deserialize(convertLegacyColors(hoverText));
        clickableComponent = clickableComponent.hoverEvent(
            net.kyori.adventure.text.event.HoverEvent.showText(hoverComponent));
        
        return clickableComponent;
    }
    
    private String getDimensionFromWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            switch (world.getEnvironment()) {
                case NORMAL:
                    return "minecraft:overworld";
                case NETHER:
                    return "minecraft:the_nether";
                case THE_END:
                    return "minecraft:the_end";
                default:
                    return "minecraft:overworld";
            }
        }
        return "minecraft:overworld";
    }
    
    public Component createClickableVillagerTpComponent(String world, double x, double y, double z) {
        String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
        String dimension = getDimensionFromWorld(world);
        String command = String.format("/execute in %s run tp @s %.0f %.0f %.0f", dimension, x, y, z);
        
        String hoverTextRaw;
        if (!messages.containsKey("tracker-villager-tp-hover")) {
            hoverTextRaw = getRaw("tracker-coords-hover");
        } else {
            hoverTextRaw = getRaw("tracker-villager-tp-hover");
        }
        String hoverText = hoverTextRaw.replace("{world}", world);
        
        String textRaw;
        if (!messages.containsKey("tracker-villager-tp-click")) {
            textRaw = getRaw("tracker-entry-coords-click");
        } else {
            textRaw = getRaw("tracker-villager-tp-click");
        }
        String text = textRaw.replace("{coords}", coords);
        
        String miniMessageText = String.format("<click:run_command:'%s'><hover:show_text:'%s'>%s</hover></click>", 
            command, hoverText.replace("'", "''"), convertLegacyColors(text));
        return miniMessage.deserialize(miniMessageText);
    }
    
    public net.md_5.bungee.api.chat.TextComponent createClickableVillagerTpComponentSpigot(String world, double x, double y, double z) {
        String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
        String dimension = getDimensionFromWorld(world);
        String command = String.format("/execute in %s run tp @s %.0f %.0f %.0f", dimension, x, y, z);
        
        String hoverTextRaw;
        if (!messages.containsKey("tracker-villager-tp-hover")) {
            hoverTextRaw = getRaw("tracker-coords-hover");
        } else {
            hoverTextRaw = getRaw("tracker-villager-tp-hover");
        }
        String hoverText = hoverTextRaw.replace("{world}", world);
        
        String textRaw;
        if (!messages.containsKey("tracker-villager-tp-click")) {
            textRaw = getRaw("tracker-entry-coords-click");
        } else {
            textRaw = getRaw("tracker-villager-tp-click");
        }
        String text = org.bukkit.ChatColor.translateAlternateColorCodes('&', textRaw.replace("{coords}", coords));
        
        net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
        component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
            new net.md_5.bungee.api.chat.hover.content.Text(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', hoverText))));
        
        return component;
    }
    
    @Deprecated
    public net.md_5.bungee.api.chat.TextComponent createClickableCoords(String world, double x, double y, double z) {
        String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
        String command = String.format("/tp %.0f %.0f %.0f", x, y, z);
        
        String rawText = get("tracker-entry-coords-click", "coords", coords);
        net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(rawText));
        
        String hoverText = get("tracker-coords-hover", "world", world);
        
        component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
            new net.md_5.bungee.api.chat.hover.content.Text(hoverText)));
        
        return component;
    }
    
    public void reload() {
        this.currentLanguage = plugin.getConfig().getString("language", "ru");
        loadMessages();
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
