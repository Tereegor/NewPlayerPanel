package newplayerpanel;

import newplayerpanel.commands.NPPCommand;
import newplayerpanel.messages.MessageManager;
import newplayerpanel.restrictions.RestrictionsModule;
import newplayerpanel.spawnprotect.SpawnProtectModule;
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
    private SpawnProtectModule spawnProtectModule;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        
        if (!initializeStorage()) {
            return;
        }
        
        this.messageManager = new MessageManager(this, storageProvider);
        messageManager.loadMessages();
        
        this.villagerTrackerModule = new VillagerTrackerModule(this, storageProvider, messageManager);
        this.restrictionsModule = new RestrictionsModule(this, storageProvider, messageManager);
        this.spawnProtectModule = new SpawnProtectModule(this, messageManager);
        
        registerCommands();
        
        getLogger().info("NewPlayerPanel v" + getDescription().getVersion() + " enabled!");
    }
    
    private boolean initializeStorage() {
        String storageType = getConfig().getString("storage", "H2").toUpperCase();
        
        switch (storageType) {
            case "YAML":
            case "JSON":
                this.storageProvider = new JsonStorage(this);
                getLogger().info("Using YAML/JSON file storage.");
                break;
            case "H2":
            case "SQLITE":
                this.storageProvider = new DatabaseStorage(this, "H2");
                getLogger().info("Using H2/SQLite database storage.");
                break;
            case "MYSQL":
                this.storageProvider = new DatabaseStorage(this, "MYSQL");
                getLogger().info("Using MySQL database storage.");
                break;
            case "MARIADB":
                this.storageProvider = new DatabaseStorage(this, "MARIADB");
                getLogger().info("Using MariaDB database storage.");
                break;
            default:
                this.storageProvider = new DatabaseStorage(this, "H2");
                getLogger().info("Unknown storage type '" + storageType + "', using H2 database storage.");
                break;
        }
        
        if (!storageProvider.initialize()) {
            getLogger().severe("Failed to initialize storage! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        
        return true;
    }
    
    private void registerCommands() {
        if (getCommand("npp") != null) {
            NPPCommand nppCommand = new NPPCommand(this, messageManager);
            getCommand("npp").setExecutor(nppCommand);
            getCommand("npp").setTabCompleter(nppCommand);
        }
    }
    
    @Override
    public void onDisable() {
        if (spawnProtectModule != null) {
            spawnProtectModule.onDisable();
            spawnProtectModule = null;
        }
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
    
    public void reload() {
        reloadConfig();
        
        if (messageManager != null) {
            messageManager.reload();
        }
        
        if (restrictionsModule != null) {
            restrictionsModule.reload();
        }
        
        if (villagerTrackerModule != null) {
            villagerTrackerModule.reload();
        }
        
        if (spawnProtectModule != null) {
            spawnProtectModule.reload();
        }
        
        getLogger().info("NewPlayerPanel configuration reloaded!");
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
    
    public SpawnProtectModule getSpawnProtectModule() {
        return spawnProtectModule;
    }
}
