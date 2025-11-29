package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictionsManager {
    
    private final JavaPlugin plugin;
    private final StorageProvider storageProvider;
    private final MessageManager messageManager;
    private File restrictionsFile;
    private FileConfiguration restrictionsConfig;
    private List<Restriction> restrictions;
    private final Map<UUID, List<PlayerRestriction>> playerRestrictionsCache;
    private int cleanupTaskId;
    private long serverStartTime;
    
    public RestrictionsManager(JavaPlugin plugin, StorageProvider storageProvider, MessageManager messageManager) {
        this.plugin = plugin;
        this.storageProvider = storageProvider;
        this.messageManager = messageManager;
        this.restrictions = new ArrayList<>();
        this.playerRestrictionsCache = new ConcurrentHashMap<>();
        this.restrictionsFile = new File(plugin.getDataFolder(), "restrictions.yml");
        this.serverStartTime = System.currentTimeMillis();
    }
    
    public void loadRestrictions() {
        if (!restrictionsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("restrictions.yml")) {
                if (in != null) {
                    Files.copy(in, restrictionsFile.toPath());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create restrictions.yml: " + e.getMessage());
            }
        }
        
        restrictionsConfig = YamlConfiguration.loadConfiguration(restrictionsFile);
        restrictions.clear();
        
        List<?> restrictionsList = restrictionsConfig.getList("restrictions");
        if (restrictionsList != null) {
            for (Object obj : restrictionsList) {
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                    try {
                        Restriction restriction = Restriction.fromMap(map);
                        restrictions.add(restriction);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error loading restriction: " + e.getMessage());
                    }
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + restrictions.size() + " restrictions.");
        
        loadPlayerRestrictionsFromStorage();
        startCleanupTask();
    }
    
    private void loadPlayerRestrictionsFromStorage() {
        playerRestrictionsCache.clear();
        playerRestrictionsCache.putAll(storageProvider.loadPlayerRestrictions());
        plugin.getLogger().info("Loaded " + playerRestrictionsCache.size() + " players with active restrictions.");
    }
    
    private void startCleanupTask() {
        if (cleanupTaskId != 0) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
        }
        cleanupTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            cleanupExpiredRestrictions();
        }, 20L, 20L * 10L);
    }
    
    private void cleanupExpiredRestrictions() {
        playerRestrictionsCache.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(PlayerRestriction::isExpired);
            return entry.getValue().isEmpty();
        });
        
        storageProvider.cleanupExpiredRestrictions();
    }
    
    public void saveRestrictions() {
        try {
            restrictionsConfig.save(restrictionsFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save restrictions.yml: " + e.getMessage());
        }
    }
    
    public List<Restriction> getRestrictions() {
        return new ArrayList<>(restrictions);
    }
    
    public Restriction getRestrictionByName(String name) {
        return restrictions.stream()
            .filter(r -> r.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }
    
    public void addPlayerRestriction(UUID playerUUID, String restrictionName, long durationSeconds) {
        List<PlayerRestriction> playerRests = playerRestrictionsCache.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        
        playerRests.removeIf(pr -> pr.getRestrictionName().equalsIgnoreCase(restrictionName));
        
        PlayerRestriction newRestriction = new PlayerRestriction(playerUUID, restrictionName, durationSeconds);
        playerRests.add(newRestriction);
        
        boolean isPermanent = durationSeconds == -1;
        long expireTime = isPermanent ? Long.MAX_VALUE : System.currentTimeMillis() + (durationSeconds * 1000L);
        storageProvider.savePlayerRestriction(playerUUID, restrictionName, expireTime, isPermanent);
    }
    
    public long getPlayerRestrictionTime(UUID playerUUID, String restrictionName) {
        List<PlayerRestriction> playerRests = playerRestrictionsCache.get(playerUUID);
        if (playerRests != null) {
            for (PlayerRestriction pr : playerRests) {
                if (pr.getRestrictionName().equalsIgnoreCase(restrictionName) && !pr.isExpired()) {
                    return pr.getDurationSeconds();
                }
            }
        }
        return -1;
    }
    
    public void removePlayerRestriction(UUID playerUUID, String restrictionName) {
        List<PlayerRestriction> playerRests = playerRestrictionsCache.get(playerUUID);
        if (playerRests != null) {
            playerRests.removeIf(pr -> pr.getRestrictionName().equalsIgnoreCase(restrictionName));
            if (playerRests.isEmpty()) {
                playerRestrictionsCache.remove(playerUUID);
            }
        }
        
        storageProvider.removePlayerRestriction(playerUUID, restrictionName);
    }
    
    public boolean hasPlayerRestriction(UUID playerUUID, String restrictionName) {
        List<PlayerRestriction> playerRests = playerRestrictionsCache.get(playerUUID);
        if (playerRests != null) {
            for (PlayerRestriction pr : playerRests) {
                if (pr.getRestrictionName().equalsIgnoreCase(restrictionName) && !pr.isExpired()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean shouldApplyDefaultRestriction(UUID playerUUID, Restriction restriction) {
        if (!restriction.isDefault()) {
            return false;
        }
        
        if (hasPlayerRestriction(playerUUID, restriction.getName())) {
            return false;
        }
        
        int timeSeconds = restriction.getTimeSeconds();
        
        if (timeSeconds <= 0) {
            return true;
        }
        
        long elapsedSeconds = (System.currentTimeMillis() - serverStartTime) / 1000L;
        return elapsedSeconds < timeSeconds;
    }
    
    public long getDefaultRestrictionRemainingTime(String restrictionName) {
        Restriction restriction = getRestrictionByName(restrictionName);
        if (restriction == null || !restriction.isDefault()) {
            return 0;
        }
        
        int timeSeconds = restriction.getTimeSeconds();
        
        if (timeSeconds <= 0) {
            return -1;
        }
        
        long elapsedSeconds = (System.currentTimeMillis() - serverStartTime) / 1000L;
        long remaining = timeSeconds - elapsedSeconds;
        return remaining > 0 ? remaining : 0;
    }
    
    public long getRestrictionRemainingTime(UUID playerUUID, String restrictionName) {
        List<PlayerRestriction> playerRests = getPlayerRestrictions(playerUUID);
        for (PlayerRestriction pr : playerRests) {
            if (pr.getRestrictionName().equalsIgnoreCase(restrictionName)) {
                return pr.getRemainingSeconds();
            }
        }
        
        return getDefaultRestrictionRemainingTime(restrictionName);
    }
    
    public boolean isRestricted(UUID playerUUID, Restriction restriction) {
        return hasPlayerRestriction(playerUUID, restriction.getName()) || 
               shouldApplyDefaultRestriction(playerUUID, restriction);
    }
    
    public List<PlayerRestriction> getPlayerRestrictions(UUID playerUUID) {
        List<PlayerRestriction> playerRests = playerRestrictionsCache.get(playerUUID);
        if (playerRests != null) {
            playerRests.removeIf(PlayerRestriction::isExpired);
            return new ArrayList<>(playerRests);
        }
        return new ArrayList<>();
    }
    
    public void reloadRestrictions() {
        loadRestrictions();
    }
    
    public void onDisable() {
        if (cleanupTaskId != 0) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
        }
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
}
