package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.storage.StorageProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RestrictionsModule {
    
    private final RestrictionsManager restrictionsManager;
    
    public RestrictionsModule(JavaPlugin plugin, StorageProvider storageProvider, MessageManager messageManager) {
        this.restrictionsManager = new RestrictionsManager(plugin, storageProvider, messageManager);
        restrictionsManager.loadRestrictions();
        
        plugin.getServer().getPluginManager().registerEvents(
            new RestrictionsListener(restrictionsManager, messageManager), plugin);
        
        if (plugin.getCommand("restrict") != null) {
            RestrictCommand restrictCommand = new RestrictCommand(restrictionsManager, messageManager);
            plugin.getCommand("restrict").setExecutor(restrictCommand);
            plugin.getCommand("restrict").setTabCompleter(restrictCommand);
        }
        
        if (plugin.getCommand("unrestrict") != null) {
            UnrestrictCommand unrestrictCommand = new UnrestrictCommand(restrictionsManager, messageManager);
            plugin.getCommand("unrestrict").setExecutor(unrestrictCommand);
            plugin.getCommand("unrestrict").setTabCompleter(unrestrictCommand);
        }
        
        if (plugin.getCommand("restrictions") != null) {
            plugin.getCommand("restrictions").setExecutor(
                new RestrictionsCommand(restrictionsManager, messageManager));
        }
    }
    
    public void onDisable() {
        restrictionsManager.onDisable();
        restrictionsManager.saveRestrictions();
    }
    
    public RestrictionsManager getRestrictionsManager() {
        return restrictionsManager;
    }
}
