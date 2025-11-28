package newplayerpanel.villagertracker;

import org.bukkit.plugin.java.JavaPlugin;

public class VillagerTrackerModule {
    
    private final VillagerDataManager dataManager;
    
    public VillagerTrackerModule(JavaPlugin plugin) {
        this.dataManager = new VillagerDataManager(plugin);
        dataManager.loadData();
        
        plugin.getServer().getPluginManager().registerEvents(
            new VillagerDeathListener(plugin, dataManager), plugin);
        
        if (plugin.getCommand("villagerhistory") != null) {
            plugin.getCommand("villagerhistory").setExecutor(
                new VillagerHistoryCommand(dataManager));
        }
    }
    
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }
    }
    
    public VillagerDataManager getDataManager() {
        return dataManager;
    }
}

