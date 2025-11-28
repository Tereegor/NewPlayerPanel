package newplayerpanel.tntprotection;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TNTProtectionManager {
    
    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    private final Map<UUID, Long> playerJoinTimes;
    private int minHours;
    private final Map<String, Integer> restrictionHours;
    
    public TNTProtectionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerJoinTimes = new HashMap<>();
        this.restrictionHours = new HashMap<>();
        this.configFile = new File(plugin.getDataFolder(), "tntprotection.yml");
    }
    
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("tntprotection.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось создать файл tntprotection.yml: " + e.getMessage());
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        minHours = config.getInt("min-hours", 24);
        
        restrictionHours.clear();
        if (config.contains("restrictions")) {
            for (String key : config.getConfigurationSection("restrictions").getKeys(false)) {
                String path = "restrictions." + key;
                if (config.getBoolean(path + ".enabled", true)) {
                    int hours = config.getInt(path + ".min-hours", minHours);
                    restrictionHours.put(key, hours);
                }
            }
        }
    }
    
    public void onPlayerJoin(UUID playerUUID) {
        if (!playerJoinTimes.containsKey(playerUUID)) {
            playerJoinTimes.put(playerUUID, System.currentTimeMillis());
        }
    }
    
    public long getPlayerPlayTimeHours(UUID playerUUID) {
        Long joinTime = playerJoinTimes.get(playerUUID);
        if (joinTime == null) {
            joinTime = System.currentTimeMillis();
            playerJoinTimes.put(playerUUID, joinTime);
        }
        
        long playTimeMs = System.currentTimeMillis() - joinTime;
        return playTimeMs / (1000L * 60L * 60L);
    }
    
    public boolean canUseTNT(UUID playerUUID) {
        long hours = getPlayerPlayTimeHours(playerUUID);
        return hours >= minHours;
    }
    
    public boolean canUseRestriction(UUID playerUUID, String restrictionKey) {
        Integer requiredHours = restrictionHours.get(restrictionKey);
        if (requiredHours == null || requiredHours == 0) {
            return true;
        }
        
        long hours = getPlayerPlayTimeHours(playerUUID);
        return hours >= requiredHours;
    }
    
    public int getRequiredHours(String restrictionKey) {
        return restrictionHours.getOrDefault(restrictionKey, minHours);
    }
    
    public int getMinHours() {
        return minHours;
    }
    
    public void removePlayer(UUID playerUUID) {
        playerJoinTimes.remove(playerUUID);
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось сохранить tntprotection.yml: " + e.getMessage());
        }
    }
}

