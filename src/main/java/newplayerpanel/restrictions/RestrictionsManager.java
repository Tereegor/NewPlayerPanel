package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
        Map<UUID, List<PlayerRestriction>> loaded = storageProvider.loadPlayerRestrictions();
        
        for (Map.Entry<UUID, List<PlayerRestriction>> entry : loaded.entrySet()) {
            List<PlayerRestriction> validRestrictions = new ArrayList<>();
            for (PlayerRestriction pr : entry.getValue()) {
                Restriction restriction = getRestrictionByName(pr.getRestrictionName());
                if (restriction != null) {
                    validRestrictions.add(pr);
                } else {
                    plugin.getLogger().warning("Player restriction '" + pr.getRestrictionName() + 
                        "' for player " + entry.getKey() + " references non-existent restriction, removing it.");
                    storageProvider.removePlayerRestriction(entry.getKey(), pr.getRestrictionName());
                }
            }
            if (!validRestrictions.isEmpty()) {
                playerRestrictionsCache.put(entry.getKey(), validRestrictions);
            }
        }
        
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
            List<String> headerComments = new ArrayList<>();
            if (restrictionsFile.exists()) {
                try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(
                        restrictionsFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("restrictions:")) {
                            break;
                        }
                        if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                            headerComments.add(line);
                        }
                    }
                }
            }
            
            if (headerComments.isEmpty()) {
                headerComments.add("# Restrictions configuration");
                headerComments.add("#");
                headerComments.add("# Each restriction has:");
                headerComments.add("# - name: unique identifier");
                headerComments.add("# - type: EQUIPMENT, ITEM, ENTITY, or COMMAND");
                headerComments.add("# - actions: comma-separated or list (DAMAGE, USE, DROP, PICKUP, EQUIP, EXECUTE)");
                headerComments.add("# - item/entity/command: list of targets");
                headerComments.add("# - time: ");
                headerComments.add("#   -1 = permanent (always active)");
                headerComments.add("#   0 = permanent (always active, same as -1)");
                headerComments.add("#   >0 = active for N seconds since server start");
                headerComments.add("# - default: true = applies to all players by default");
                        headerComments.add("");
                    }
                    
                    List<Restriction> sortedRestrictions = new ArrayList<>(restrictions);
                    sortedRestrictions.sort((a, b) -> {
                        if (a.isDefault() != b.isDefault()) {
                            return b.isDefault() ? 1 : -1;
                        }
                        return a.getName().compareToIgnoreCase(b.getName());
                    });
                    
                    StringBuilder content = new StringBuilder();
                    
                    for (String comment : headerComments) {
                        content.append(comment).append("\n");
                    }
                    
                    content.append("restrictions:\n");
                    
                    for (int i = 0; i < sortedRestrictions.size(); i++) {
                Restriction restriction = sortedRestrictions.get(i);
                content.append("  - name: ").append(restriction.getName()).append("\n");
                content.append("    type: ").append(restriction.getType().name()).append("\n");
                
                List<String> actionsList = new ArrayList<>(restriction.getActions());
                actionsList.sort(String::compareToIgnoreCase);
                if (actionsList.size() == 1) {
                    content.append("    actions: ").append(actionsList.get(0)).append("\n");
                } else {
                    content.append("    actions:\n");
                    for (String action : actionsList) {
                        content.append("      - ").append(action).append("\n");
                    }
                }
                
                switch (restriction.getType()) {
                    case EQUIPMENT:
                    case ITEM:
                        if (!restriction.getItems().isEmpty()) {
                            List<String> sortedItems = new ArrayList<>(restriction.getItems());
                            sortedItems.sort(String::compareToIgnoreCase);
                            content.append("    item:\n");
                            for (String item : sortedItems) {
                                content.append("      - ").append(item).append("\n");
                            }
                        }
                        break;
                    case ENTITY:
                        if (!restriction.getEntities().isEmpty()) {
                            List<String> sortedEntities = new ArrayList<>(restriction.getEntities());
                            sortedEntities.sort(String::compareToIgnoreCase);
                            content.append("    entity:\n");
                            for (String entity : sortedEntities) {
                                content.append("      - ").append(entity).append("\n");
                            }
                        }
                        break;
                    case COMMAND:
                        if (!restriction.getCommands().isEmpty()) {
                            List<String> sortedCommands = new ArrayList<>(restriction.getCommands());
                            sortedCommands.sort(String::compareToIgnoreCase);
                            content.append("    command:\n");
                            for (String command : sortedCommands) {
                                content.append("      - ").append(command).append("\n");
                            }
                        }
                        break;
                }
                
                        content.append("    time: ").append(restriction.getTimeSeconds()).append("\n");
                        content.append("    default: ").append(restriction.isDefault()).append("\n");
                        
                        if (i < sortedRestrictions.size() - 1) {
                    content.append("\n");
                }
            }
            
            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                    restrictionsFile.toPath(), java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE)) {
                writer.write(content.toString());
            }
            
            plugin.getLogger().fine("Saved " + restrictions.size() + " restrictions to restrictions.yml");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save restrictions.yml: " + e.getMessage());
            plugin.getLogger().severe("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
        }
    }
    
    public boolean addNewRestriction(String name, String type, String actionsStr, List<String> targets, int timeSeconds, boolean isDefault) {
        if (getRestrictionByName(name) != null) {
            return false;
        }
        
        try {
            Restriction.RestrictionType restrictionType = Restriction.RestrictionType.valueOf(type);
            
            Set<String> actions = new HashSet<>();
            for (String action : actionsStr.split(",")) {
                actions.add(action.trim().toUpperCase());
            }
            
            List<String> items = null;
            List<String> entities = null;
            List<String> commands = null;
            
            switch (restrictionType) {
                case EQUIPMENT:
                case ITEM:
                    items = new ArrayList<>(targets);
                    break;
                case ENTITY:
                    entities = new ArrayList<>(targets);
                    break;
                case COMMAND:
                    commands = new ArrayList<>(targets);
                    break;
            }
            
            Restriction newRestriction = new Restriction(name, restrictionType, actions, items, entities, commands, timeSeconds, isDefault);
            restrictions.add(newRestriction);
            saveRestrictions();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating restriction: " + e.getMessage());
            return false;
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
    
    private long getPlayerPlayTimeSeconds(UUID playerUUID) {
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null) {
            try {
                int ticks = onlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE);
                return ticks / 20L;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get playtime for player " + playerUUID + ": " + e.getMessage());
            }
        }
        
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        if (offlinePlayer instanceof Player) {
            try {
                int ticks = ((Player) offlinePlayer).getStatistic(Statistic.PLAY_ONE_MINUTE);
                return ticks / 20L;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get playtime for offline player " + playerUUID + ": " + e.getMessage());
            }
        }
        
        return 0;
    }
    
    public boolean shouldApplyDefaultRestriction(UUID playerUUID, Restriction restriction) {
        if (!restriction.isDefault()) {
            return false;
        }
        
        if (hasPlayerRestriction(playerUUID, restriction.getName())) {
            return false;
        }
        
        int timeSeconds = restriction.getTimeSeconds();
        
        if (timeSeconds == -1) {
            return true;
        }
        
        if (timeSeconds == 0) {
            return false;
        }
        
        long playerPlayTimeSeconds = getPlayerPlayTimeSeconds(playerUUID);
        return playerPlayTimeSeconds < timeSeconds;
    }
    
    public long getDefaultRestrictionRemainingTime(UUID playerUUID, String restrictionName) {
        Restriction restriction = getRestrictionByName(restrictionName);
        if (restriction == null || !restriction.isDefault()) {
            return 0;
        }
        
        int timeSeconds = restriction.getTimeSeconds();
        
        if (timeSeconds == -1) {
            return -1;
        }
        
        if (timeSeconds == 0) {
            return 0;
        }
        
        long playerPlayTimeSeconds = getPlayerPlayTimeSeconds(playerUUID);
        long remaining = timeSeconds - playerPlayTimeSeconds;
        return remaining > 0 ? remaining : 0;
    }
    
    public long getRestrictionRemainingTime(UUID playerUUID, String restrictionName) {
        List<PlayerRestriction> playerRests = getPlayerRestrictions(playerUUID);
        for (PlayerRestriction pr : playerRests) {
            if (pr.getRestrictionName().equalsIgnoreCase(restrictionName)) {
                return pr.getRemainingSeconds();
            }
        }
        
        return getDefaultRestrictionRemainingTime(playerUUID, restrictionName);
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
    
    public List<Restriction> getActiveDefaultRestrictions(UUID playerUUID) {
        List<Restriction> activeDefaults = new ArrayList<>();
        for (Restriction restriction : restrictions) {
            if (shouldApplyDefaultRestriction(playerUUID, restriction)) {
                activeDefaults.add(restriction);
            }
        }
        return activeDefaults;
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
    
    public long getServerStartTime() {
        return serverStartTime;
    }
}
