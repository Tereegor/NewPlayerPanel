package newplayerpanel;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.restrictions.RestrictionsModule;
import newplayerpanel.storage.DatabaseStorage;
import newplayerpanel.storage.JsonStorage;
import newplayerpanel.storage.StorageProvider;
import newplayerpanel.villagertracker.VillagerTrackerModule;
import org.bukkit.plugin.java.JavaPlugin;

public class NewPlayerPanel extends JavaPlugin {
    
    private StorageProvider storageProvider;
    private MessageManager messageManager;
    private VillagerTrackerModule villagerTrackerModule;
    private RestrictionsModule restrictionsModule;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        
        String storageType = getConfig().getString("storage", "database").toLowerCase();
        
        if (storageType.equals("json")) {
            this.storageProvider = new JsonStorage(this);
            getLogger().info("Using JSON file storage.");
        } else {
            this.storageProvider = new DatabaseStorage(this);
            getLogger().info("Using SQLite database storage.");
        }
        
        if (!storageProvider.initialize()) {
            getLogger().severe("Failed to initialize storage! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.messageManager = new MessageManager(this, storageProvider);
        messageManager.loadMessages();
        
        this.villagerTrackerModule = new VillagerTrackerModule(this, storageProvider, messageManager);
        this.restrictionsModule = new RestrictionsModule(this, storageProvider, messageManager);
        
        getLogger().info("NewPlayerPanel v" + getDescription().getVersion() + " enabled!");
    }
    
    @Override
    public void onDisable() {
        if (restrictionsModule != null) {
            restrictionsModule.onDisable();
            restrictionsModule = null;
        }
        if (villagerTrackerModule != null) {
            villagerTrackerModule.onDisable();
            villagerTrackerModule = null;
        }
        if (storageProvider != null) {
            storageProvider.shutdown();
            storageProvider = null;
        }
        messageManager = null;
        
        getLogger().info("NewPlayerPanel disabled.");
    }
    
    public StorageProvider getStorageProvider() {
        return storageProvider;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public VillagerTrackerModule getVillagerTrackerModule() {
        return villagerTrackerModule;
    }
    
    public RestrictionsModule getRestrictionsModule() {
        return restrictionsModule;
    }
}
