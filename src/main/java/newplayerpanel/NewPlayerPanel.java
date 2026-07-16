package newplayerpanel;

import newplayerpanel.commands.NPPCommand;
import newplayerpanel.messages.MessageManager;
import newplayerpanel.restrictions.*;
import newplayerpanel.spawnprotect.*;
import newplayerpanel.storage.DatabaseStorage;
import newplayerpanel.villagertracker.*;
import org.bukkit.plugin.java.JavaPlugin;

public class NewPlayerPanel extends JavaPlugin {
    
    private DatabaseStorage database;
    private MessageManager messageManager;
    private VillagerDataManager villagerDataManager;
    private RestrictionsManager restrictionsManager;
    private SpawnProtectManager spawnProtectManager;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        
        this.database = new DatabaseStorage(this);
        if (!database.initialize()) {
            getLogger().severe("Failed to initialize SQLite database! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.messageManager = new MessageManager(this);
        messageManager.loadMessages();
        
        this.villagerDataManager = new VillagerDataManager(this, database);
        getServer().getPluginManager().registerEvents(new VillagerDeathListener(this, villagerDataManager, messageManager), this);
        if (getCommand("history") != null) {
            HistoryCommand historyCommand = new HistoryCommand(villagerDataManager, messageManager, this);
            getCommand("history").setExecutor(historyCommand);
            getCommand("history").setTabCompleter(historyCommand);
        }
        
        this.restrictionsManager = new RestrictionsManager(this, database, messageManager);
        restrictionsManager.loadRestrictions();
        getServer().getPluginManager().registerEvents(new RestrictionsListener(restrictionsManager, messageManager, this), this);
        if (getCommand("restrict") != null) {
            RestrictCommand restrictCommand = new RestrictCommand(restrictionsManager, messageManager);
            getCommand("restrict").setExecutor(restrictCommand);
            getCommand("restrict").setTabCompleter(restrictCommand);
        }
        if (getCommand("unrestrict") != null) {
            UnrestrictCommand unrestrictCommand = new UnrestrictCommand(restrictionsManager, messageManager);
            getCommand("unrestrict").setExecutor(unrestrictCommand);
            getCommand("unrestrict").setTabCompleter(unrestrictCommand);
        }
        if (getCommand("restrictions") != null) {
            getCommand("restrictions").setExecutor(new RestrictionsCommand(restrictionsManager, messageManager));
        }
        
        this.spawnProtectManager = new SpawnProtectManager(this);
        spawnProtectManager.loadZones();
        if (spawnProtectManager.isEnabled()) {
            getServer().getPluginManager().registerEvents(new SpawnProtectListener(spawnProtectManager, messageManager), this);
        }
        if (getCommand("spawnprotect") != null) {
            SpawnProtectCommand spawnProtectCommand = new SpawnProtectCommand(spawnProtectManager, messageManager);
            getCommand("spawnprotect").setExecutor(spawnProtectCommand);
            getCommand("spawnprotect").setTabCompleter(spawnProtectCommand);
            getServer().getPluginManager().registerEvents(spawnProtectCommand, this);
        }
        
        if (getCommand("npp") != null) {
            NPPCommand nppCommand = new NPPCommand(this, messageManager);
            getCommand("npp").setExecutor(nppCommand);
            getCommand("npp").setTabCompleter(nppCommand);
        }
        
        getLogger().info("NewPlayerPanel v" + getDescription().getVersion() + " enabled!");
    }
    
    @Override
    public void onDisable() {
        if (restrictionsManager != null) {
            restrictionsManager.onDisable();
            restrictionsManager.saveRestrictions();
        }
        if (database != null) database.shutdown();
        getLogger().info("NewPlayerPanel disabled.");
    }
    
    public void reload() {
        reloadConfig();
        if (messageManager != null) messageManager.reload();
        if (restrictionsManager != null) restrictionsManager.reloadRestrictions();
        if (villagerDataManager != null) villagerDataManager.reload();
        if (spawnProtectManager != null) spawnProtectManager.reload();
        getLogger().info("NewPlayerPanel configuration reloaded!");
    }
    
    public DatabaseStorage getDatabase() { return database; }
    public MessageManager getMessageManager() { return messageManager; }
    public VillagerDataManager getVillagerDataManager() { return villagerDataManager; }
    public RestrictionsManager getRestrictionsManager() { return restrictionsManager; }
    public SpawnProtectManager getSpawnProtectManager() { return spawnProtectManager; }
}
