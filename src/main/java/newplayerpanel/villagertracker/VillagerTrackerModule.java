package newplayerpanel.villagertracker;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.storage.StorageProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VillagerTrackerModule {
    
    private final VillagerDataManager dataManager;
    
    public VillagerTrackerModule(JavaPlugin plugin, StorageProvider storageProvider, MessageManager messageManager) {
        this.dataManager = new VillagerDataManager(plugin, storageProvider);
        
        plugin.getServer().getPluginManager().registerEvents(
            new VillagerDeathListener(plugin, dataManager, messageManager), plugin);
        
        if (plugin.getCommand("history") != null) {
            HistoryCommand historyCommand = new HistoryCommand(dataManager, messageManager, plugin);
            plugin.getCommand("history").setExecutor(historyCommand);
            plugin.getCommand("history").setTabCompleter(historyCommand);
        }
    }
    
    public void reload() {
        dataManager.reload();
    }
    
    public void onDisable() {
    }
    
    public VillagerDataManager getDataManager() {
        return dataManager;
    }
}
