package newplayerpanel.villagertracker;

import newplayerpanel.storage.StorageProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class VillagerDataManager {
    
    private final JavaPlugin plugin;
    private final StorageProvider storageProvider;
    
    public VillagerDataManager(JavaPlugin plugin, StorageProvider storageProvider) {
        this.plugin = plugin;
        this.storageProvider = storageProvider;
    }
    
    public void addRecord(VillagerDeathRecord record) {
        storageProvider.addVillagerDeath(record);
    }
    
    public List<VillagerDeathRecord> getRecords() {
        return storageProvider.getVillagerDeaths();
    }
    
    public List<VillagerDeathRecord> getRecordsByPlayer(String playerName) {
        return storageProvider.getVillagerDeathsByPlayer(playerName);
    }
    
    public List<VillagerDeathRecord> getRecordsByCoordinates(double x, double y, double z, String world) {
        return storageProvider.getVillagerDeathsByCoords(x, y, z, world);
    }
    
    public List<VillagerDeathRecord> getRecordsByPlayerAndCoordinates(String playerName, double x, double y, double z, String world) {
        return storageProvider.getVillagerDeathsByPlayerAndCoords(playerName, x, y, z, world);
    }
    
    public int clearOldRecords(long olderThanTimestamp) {
        return storageProvider.clearOldVillagerDeaths(olderThanTimestamp);
    }
    
    public void reload() {
    }
    
    public void loadData() {
    }
    
    public void saveData() {
    }
}
