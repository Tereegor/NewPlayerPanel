package newplayerpanel.restrictions;

import org.bukkit.plugin.java.JavaPlugin;

public class RestrictionsModule {
    
    private final RestrictionsManager restrictionsManager;
    
    public RestrictionsModule(JavaPlugin plugin) {
        this.restrictionsManager = new RestrictionsManager(plugin);
        restrictionsManager.loadRestrictions();
        
        plugin.getServer().getPluginManager().registerEvents(
            new RestrictionsListener(restrictionsManager), plugin);
        
        if (plugin.getCommand("restrict") != null) {
            plugin.getCommand("restrict").setExecutor(
                new RestrictCommand(restrictionsManager));
            plugin.getCommand("restrict").setTabCompleter(
                new RestrictCommand(restrictionsManager));
        }
        
        if (plugin.getCommand("unrestrict") != null) {
            plugin.getCommand("unrestrict").setExecutor(
                new UnrestrictCommand(restrictionsManager));
            plugin.getCommand("unrestrict").setTabCompleter(
                new UnrestrictCommand(restrictionsManager));
        }
        
        if (plugin.getCommand("restrictions") != null) {
            plugin.getCommand("restrictions").setExecutor(
                new RestrictionsCommand(restrictionsManager));
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

