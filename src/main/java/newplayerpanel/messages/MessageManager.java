package newplayerpanel.messages;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import newplayerpanel.storage.StorageProvider;
import org.bukkit.ChatColor;
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
    private final StorageProvider storageProvider;
    private final Map<String, String> messages;
    private String currentLanguage;
    
    public MessageManager(JavaPlugin plugin, StorageProvider storageProvider) {
        this.plugin = plugin;
        this.storageProvider = storageProvider;
        this.messages = new HashMap<>();
        this.currentLanguage = plugin.getConfig().getString("language", "ru");
    }
    
    public void loadMessages() {
        messages.clear();
        
        if (!storageProvider.messagesExist(currentLanguage)) {
            loadMessagesFromFiles();
        }
        
        messages.putAll(storageProvider.loadMessages(currentLanguage));
        
        if (messages.isEmpty()) {
            loadMessagesFromFilesDirectly();
        }
        
        plugin.getLogger().info("Loaded " + messages.size() + " messages for language: " + currentLanguage);
    }
    
    private void loadMessagesFromFiles() {
        String[] languages = {"ru", "en"};
        
        for (String lang : languages) {
            String fileName = "messages_" + lang + ".yml";
            InputStream stream = plugin.getResource(fileName);
            
            if (stream == null) {
                plugin.getLogger().warning("Could not find " + fileName);
                continue;
            }
            
            FileConfiguration langConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
            
            if (langConfig.isConfigurationSection("messages")) {
                for (String key : langConfig.getConfigurationSection("messages").getKeys(false)) {
                    String value = langConfig.getString("messages." + key, "");
                    storageProvider.saveMessage(lang, key, value);
                }
            }
        }
    }
    
    private void loadMessagesFromFilesDirectly() {
        String fileName = "messages_" + currentLanguage + ".yml";
        InputStream stream = plugin.getResource(fileName);
        
        if (stream == null) {
            stream = plugin.getResource("messages_ru.yml");
        }
        
        if (stream != null) {
            FileConfiguration langConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
            
            if (langConfig.isConfigurationSection("messages")) {
                for (String key : langConfig.getConfigurationSection("messages").getKeys(false)) {
                    messages.put(key, langConfig.getString("messages." + key, ""));
                }
            }
        }
    }
    
    public String getRaw(String key) {
        return messages.getOrDefault(key, key);
    }
    
    public String get(String key) {
        return ChatColor.translateAlternateColorCodes('&', messages.getOrDefault(key, key));
    }
    
    public String get(String key, Object... replacements) {
        String message = messages.getOrDefault(key, key);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                message = message.replace("{" + placeholder + "}", value);
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public TextComponent createClickableCoords(String world, double x, double y, double z) {
        String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
        String command = String.format("/tp %.0f %.0f %.0f", x, y, z);
        
        String rawText = get("tracker-entry-coords-click", "coords", coords);
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(rawText));
        
        String hoverText = get("tracker-coords-hover", "world", world);
        
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
        
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
