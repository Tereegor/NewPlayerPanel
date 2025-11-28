package newplayerpanel.tntprotection;

import newplayerpanel.NewPlayerPanel;
import newplayerpanel.restrictions.RestrictionsManager;

public class TNTProtectionModule {
    
    private final NewPlayerPanel plugin;
    private TNTProtectionManager protectionManager;
    
    public TNTProtectionModule(NewPlayerPanel plugin) {
        this.plugin = plugin;
    }
    
    public void onEnable() {
        this.protectionManager = new TNTProtectionManager(plugin);
        protectionManager.loadConfig();
        
        RestrictionsManager restrictionsManager = null;
        if (plugin.getRestrictionsModule() != null) {
            restrictionsManager = plugin.getRestrictionsModule().getRestrictionsManager();
        }
        
        plugin.getServer().getPluginManager().registerEvents(
            new TNTProtectionListener(protectionManager, restrictionsManager), plugin);
        
        if (plugin.getCommand("tntprotection") != null) {
            TNTProtectionCommand command = new TNTProtectionCommand(protectionManager);
            plugin.getCommand("tntprotection").setExecutor(command);
            plugin.getCommand("tntprotection").setTabCompleter(command);
        }
    }
    
    public void onDisable() {
        if (protectionManager != null) {
            protectionManager.saveConfig();
        }
    }
    
    public TNTProtectionManager getProtectionManager() {
        return protectionManager;
    }
}

