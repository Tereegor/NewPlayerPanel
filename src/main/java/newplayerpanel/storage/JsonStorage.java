package newplayerpanel.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import newplayerpanel.restrictions.PlayerRestriction;
import newplayerpanel.villagertracker.VillagerDeathRecord;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class JsonStorage implements StorageProvider {
    
    private final JavaPlugin plugin;
    private final Gson gson;
    private File dataFolder;
    private File messagesFile;
    private File villagerDeathsFile;
    private File restrictionsFile;
    private Map<String, Map<String, String>> messagesCache = new HashMap<>();
    private List<VillagerDeathRecord> villagerDeathsCache = new ArrayList<>();
    private Map<UUID, List<RestrictionData>> restrictionsCache = new HashMap<>();
    
    public JsonStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    @Override
    public boolean initialize() {
        try {
            dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            messagesFile = new File(dataFolder, "messages.json");
            villagerDeathsFile = new File(dataFolder, "villager_deaths.json");
            restrictionsFile = new File(dataFolder, "restrictions.json");
            
            loadAllData();
            
            plugin.getLogger().info("JSON storage initialized: " + dataFolder.getAbsolutePath());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize JSON storage: " + e.getMessage());
            plugin.getLogger().severe("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
            return false;
        }
    }
    
    private void loadAllData() {
        if (messagesFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(messagesFile), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
                messagesCache = gson.fromJson(reader, type);
                if (messagesCache == null) messagesCache = new HashMap<>();
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading messages.json: " + e.getMessage());
                messagesCache = new HashMap<>();
            }
        }
        
        if (villagerDeathsFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(villagerDeathsFile), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<List<VillagerDeathRecord>>(){}.getType();
                villagerDeathsCache = gson.fromJson(reader, type);
                if (villagerDeathsCache == null) villagerDeathsCache = new ArrayList<>();
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading villager_deaths.json: " + e.getMessage());
                villagerDeathsCache = new ArrayList<>();
            }
        }
        
        if (restrictionsFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(restrictionsFile), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, List<RestrictionData>>>(){}.getType();
                Map<String, List<RestrictionData>> loaded = gson.fromJson(reader, type);
                restrictionsCache = new HashMap<>();
                if (loaded != null) {
                    loaded.forEach((key, value) -> {
                        try {
                            restrictionsCache.put(UUID.fromString(key), value);
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading restrictions.json: " + e.getMessage());
                restrictionsCache = new HashMap<>();
            }
        }
    }
    
    @Override
    public void shutdown() {
        saveAllData();
        plugin.getLogger().info("JSON storage closed.");
    }
    
    private void saveAllData() {
        saveMessages();
        saveVillagerDeaths();
        saveRestrictions();
    }
    
    private void saveMessages() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(messagesFile), StandardCharsets.UTF_8)) {
            gson.toJson(messagesCache, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving messages.json: " + e.getMessage());
        }
    }
    
    private void saveVillagerDeaths() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(villagerDeathsFile), StandardCharsets.UTF_8)) {
            gson.toJson(villagerDeathsCache, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving villager_deaths.json: " + e.getMessage());
        }
    }
    
    private void saveRestrictions() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(restrictionsFile), StandardCharsets.UTF_8)) {
            Map<String, List<RestrictionData>> toSave = new HashMap<>();
            restrictionsCache.forEach((uuid, list) -> toSave.put(uuid.toString(), list));
            gson.toJson(toSave, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving restrictions.json: " + e.getMessage());
        }
    }
    
    @Override
    public boolean messagesExist(String language) {
        return messagesCache.containsKey(language) && !messagesCache.get(language).isEmpty();
    }
    
    @Override
    public void saveMessage(String language, String key, String value) {
        messagesCache.computeIfAbsent(language, k -> new HashMap<>()).put(key, value);
        saveMessages();
    }
    
    @Override
    public Map<String, String> loadMessages(String language) {
        return new HashMap<>(messagesCache.getOrDefault(language, new HashMap<>()));
    }
    
    @Override
    public void addVillagerDeath(VillagerDeathRecord record) {
        villagerDeathsCache.add(record);
        saveVillagerDeaths();
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeaths() {
        return villagerDeathsCache.stream()
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeathsByPlayer(String playerName) {
        return villagerDeathsCache.stream()
            .filter(r -> r.getPlayerName().equalsIgnoreCase(playerName))
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeathsByCoords(double x, double y, double z, String world) {
        return villagerDeathsCache.stream()
            .filter(r -> {
                if (world != null && !r.getWorld().equalsIgnoreCase(world)) return false;
                return Math.abs(r.getX() - x) <= 2 && Math.abs(r.getY() - y) <= 2 && Math.abs(r.getZ() - z) <= 2;
            })
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeathsByPlayerAndCoords(String playerName, double x, double y, double z, String world) {
        return villagerDeathsCache.stream()
            .filter(r -> {
                if (!r.getPlayerName().equalsIgnoreCase(playerName)) return false;
                if (world != null && !r.getWorld().equalsIgnoreCase(world)) return false;
                return Math.abs(r.getX() - x) <= 2 && Math.abs(r.getY() - y) <= 2 && Math.abs(r.getZ() - z) <= 2;
            })
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .collect(Collectors.toList());
    }
    
    @Override
    public int clearOldVillagerDeaths(long olderThanTimestamp) {
        int originalSize = villagerDeathsCache.size();
        villagerDeathsCache.removeIf(r -> r.getTimestamp() < olderThanTimestamp);
        int deleted = originalSize - villagerDeathsCache.size();
        if (deleted > 0) {
            saveVillagerDeaths();
        }
        return deleted;
    }
    
    @Override
    public void savePlayerRestriction(UUID playerUUID, String restrictionName, long expireTime, boolean isPermanent) {
        List<RestrictionData> list = restrictionsCache.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        list.removeIf(r -> r.restrictionName.equalsIgnoreCase(restrictionName));
        list.add(new RestrictionData(restrictionName, expireTime, isPermanent));
        saveRestrictions();
    }
    
    @Override
    public void removePlayerRestriction(UUID playerUUID, String restrictionName) {
        List<RestrictionData> list = restrictionsCache.get(playerUUID);
        if (list != null) {
            list.removeIf(r -> r.restrictionName.equalsIgnoreCase(restrictionName));
            if (list.isEmpty()) {
                restrictionsCache.remove(playerUUID);
            }
            saveRestrictions();
        }
    }
    
    @Override
    public Map<UUID, List<PlayerRestriction>> loadPlayerRestrictions() {
        Map<UUID, List<PlayerRestriction>> result = new HashMap<>();
        long now = System.currentTimeMillis();
        
        restrictionsCache.forEach((uuid, dataList) -> {
            List<PlayerRestriction> playerList = new ArrayList<>();
            for (RestrictionData data : dataList) {
                if (data.isPermanent || data.expireTime > now) {
                    long durationSeconds = data.isPermanent ? -1 : Math.max(0, (data.expireTime - now) / 1000L);
                    playerList.add(new PlayerRestriction(uuid, data.restrictionName, durationSeconds));
                }
            }
            if (!playerList.isEmpty()) {
                result.put(uuid, playerList);
            }
        });
        
        return result;
    }
    
    @Override
    public void cleanupExpiredRestrictions() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        
        Iterator<Map.Entry<UUID, List<RestrictionData>>> iter = restrictionsCache.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, List<RestrictionData>> entry = iter.next();
            int before = entry.getValue().size();
            entry.getValue().removeIf(r -> !r.isPermanent && r.expireTime < now);
            if (entry.getValue().size() != before) changed = true;
            if (entry.getValue().isEmpty()) {
                iter.remove();
                changed = true;
            }
        }
        
        if (changed) {
            saveRestrictions();
        }
    }
    
    private static class RestrictionData {
        String restrictionName;
        long expireTime;
        boolean isPermanent;
        
        RestrictionData(String restrictionName, long expireTime, boolean isPermanent) {
            this.restrictionName = restrictionName;
            this.expireTime = expireTime;
            this.isPermanent = isPermanent;
        }
    }
}
