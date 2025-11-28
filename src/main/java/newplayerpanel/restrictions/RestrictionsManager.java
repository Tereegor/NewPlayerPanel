package newplayerpanel.restrictions;

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
    private File restrictionsFile;
    private FileConfiguration restrictionsConfig;
    private List<Restriction> restrictions;
    private final Map<UUID, List<PlayerRestriction>> playerRestrictions;
    private final Map<String, DefaultRestrictionState> defaultRestrictionStates;
    private int cleanupTaskId;
    
    public RestrictionsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.restrictions = new ArrayList<>();
        this.playerRestrictions = new ConcurrentHashMap<>();
        this.defaultRestrictionStates = new ConcurrentHashMap<>();
        this.restrictionsFile = new File(plugin.getDataFolder(), "restrictions.yml");
    }
    
    public void loadRestrictions() {
        if (!restrictionsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("restrictions.yml")) {
                if (in != null) {
                    Files.copy(in, restrictionsFile.toPath());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось создать файл restrictions.yml: " + e.getMessage());
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
                        plugin.getLogger().warning("Ошибка при загрузке ограничения: " + e.getMessage());
                    }
                }
            }
        }
        
        plugin.getLogger().info("Загружено " + restrictions.size() + " ограничений.");
        
        activateDefaultRestrictions();
        startCleanupTask();
    }
    
    private void activateDefaultRestrictions() {
        for (Restriction restriction : restrictions) {
            if (restriction.isDefault() && restriction.getTimeSeconds() != 0) {
                DefaultRestrictionState state = new DefaultRestrictionState(
                    restriction.getName(), 
                    restriction.getTimeSeconds()
                );
                defaultRestrictionStates.put(restriction.getName(), state);
            }
        }
    }
    
    private void startCleanupTask() {
        cleanupTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            cleanupExpiredRestrictions();
            cleanupExpiredDefaultRestrictions();
        }, 20L, 20L * 10L);
    }
    
    private void cleanupExpiredRestrictions() {
        playerRestrictions.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(PlayerRestriction::isExpired);
            return entry.getValue().isEmpty();
        });
    }
    
    private void cleanupExpiredDefaultRestrictions() {
        defaultRestrictionStates.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    public void saveRestrictions() {
        try {
            restrictionsConfig.save(restrictionsFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось сохранить restrictions.yml: " + e.getMessage());
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
        List<PlayerRestriction> playerRests = playerRestrictions.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        
        playerRests.removeIf(pr -> pr.getRestrictionName().equalsIgnoreCase(restrictionName));
        
        playerRests.add(new PlayerRestriction(playerUUID, restrictionName, durationSeconds));
    }
    
    public long getPlayerRestrictionTime(UUID playerUUID, String restrictionName) {
        List<PlayerRestriction> playerRests = playerRestrictions.get(playerUUID);
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
        List<PlayerRestriction> playerRests = playerRestrictions.get(playerUUID);
        if (playerRests != null) {
            playerRests.removeIf(pr -> pr.getRestrictionName().equalsIgnoreCase(restrictionName));
            if (playerRests.isEmpty()) {
                playerRestrictions.remove(playerUUID);
            }
        }
    }
    
    public boolean hasPlayerRestriction(UUID playerUUID, String restrictionName) {
        List<PlayerRestriction> playerRests = playerRestrictions.get(playerUUID);
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
        if (!restriction.isDefault() || hasPlayerRestriction(playerUUID, restriction.getName())) {
            return false;
        }
        
        DefaultRestrictionState state = defaultRestrictionStates.get(restriction.getName());
        return state != null && !state.isExpired();
    }
    
    public long getDefaultRestrictionRemainingTime(String restrictionName) {
        DefaultRestrictionState state = defaultRestrictionStates.get(restrictionName);
        if (state != null && !state.isExpired()) {
            return state.getRemainingSeconds();
        }
        return 0;
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
        return hasPlayerRestriction(playerUUID, restriction.getName()) || shouldApplyDefaultRestriction(playerUUID, restriction);
    }
    
    public List<PlayerRestriction> getPlayerRestrictions(UUID playerUUID) {
        List<PlayerRestriction> playerRests = playerRestrictions.get(playerUUID);
        if (playerRests != null) {
            playerRests.removeIf(PlayerRestriction::isExpired);
            return new ArrayList<>(playerRests);
        }
        return new ArrayList<>();
    }
    
    public void reloadRestrictions() {
        defaultRestrictionStates.clear();
        loadRestrictions();
    }
    
    public void onDisable() {
        if (cleanupTaskId != 0) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
        }
    }
}

