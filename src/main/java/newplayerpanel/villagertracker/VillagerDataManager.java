package newplayerpanel.villagertracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VillagerDataManager {
    
    private final File dataFile;
    private final Gson gson;
    private List<VillagerDeathRecord> records;
    
    public VillagerDataManager(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        this.dataFile = new File(dataFolder, "villager_deaths.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.records = new ArrayList<>();
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    public void loadData() {
        if (!dataFile.exists()) {
            records = new ArrayList<>();
            saveData();
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            Type listType = new TypeToken<ArrayList<VillagerDeathRecord>>(){}.getType();
            records = gson.fromJson(reader, listType);
            if (records == null) {
                records = new ArrayList<>();
            }
        } catch (IOException e) {
            records = new ArrayList<>();
        }
    }
    
    public void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(records, writer);
        } catch (IOException e) {
        }
    }
    
    public void addRecord(VillagerDeathRecord record) {
        records.add(record);
        saveData();
    }
    
    public List<VillagerDeathRecord> getRecords() {
        return new ArrayList<>(records);
    }
    
    public List<VillagerDeathRecord> getRecordsByPlayer(String playerName) {
        List<VillagerDeathRecord> result = new ArrayList<>();
        for (VillagerDeathRecord record : records) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                result.add(record);
            }
        }
        return result;
    }
    
    public List<VillagerDeathRecord> getRecordsByCoordinates(double x, double y, double z, String world) {
        List<VillagerDeathRecord> result = new ArrayList<>();
        for (VillagerDeathRecord record : records) {
            if (world != null && !record.getWorld().equalsIgnoreCase(world)) {
                continue;
            }
            
            double dx = Math.abs(record.getX() - x);
            double dy = Math.abs(record.getY() - y);
            double dz = Math.abs(record.getZ() - z);
            
            if (dx <= 2.0 && dy <= 2.0 && dz <= 2.0) {
                result.add(record);
            }
        }
        return result;
    }
    
    public List<VillagerDeathRecord> getRecordsByPlayerAndCoordinates(String playerName, double x, double y, double z, String world) {
        List<VillagerDeathRecord> result = new ArrayList<>();
        for (VillagerDeathRecord record : records) {
            if (!record.getPlayerName().equalsIgnoreCase(playerName)) {
                continue;
            }
            
            if (world != null && !record.getWorld().equalsIgnoreCase(world)) {
                continue;
            }
            
            double dx = Math.abs(record.getX() - x);
            double dy = Math.abs(record.getY() - y);
            double dz = Math.abs(record.getZ() - z);
            
            if (dx <= 2.0 && dy <= 2.0 && dz <= 2.0) {
                result.add(record);
            }
        }
        return result;
    }
}