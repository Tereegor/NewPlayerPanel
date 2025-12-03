package newplayerpanel.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import newplayerpanel.storage.StorageProvider;
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
        String command = String.format("/tp %.0f %.0f %.0f", x, y, z);
        
        String rawText = getRaw("tracker-entry-coords-click");
        rawText = rawText.replace("{coords}", coords);
        rawText = convertLegacyColors(rawText);
        
        Component component = miniMessage.deserialize(rawText);
        component = component.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command));
        
        String hoverText = getRaw("tracker-coords-hover");
        hoverText = hoverText.replace("{world}", world);
        hoverText = convertLegacyColors(hoverText);
        Component hoverComponent = miniMessage.deserialize(hoverText);
        component = component.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverComponent));
        
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
