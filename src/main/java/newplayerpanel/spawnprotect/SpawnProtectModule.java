package newplayerpanel.spawnprotect;

import newplayerpanel.messages.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SpawnProtectModule {
    
    private final SpawnProtectManager manager;
    
    public SpawnProtectModule(JavaPlugin plugin, MessageManager messageManager) {
        this.manager = new SpawnProtectManager(plugin);
        manager.loadZones();
        
        if (manager.isEnabled()) {
            plugin.getServer().getPluginManager().registerEvents(
                new SpawnProtectListener(manager, messageManager), plugin);
        }
    }
    
    public void reload() {
        manager.reload();
    }
    
    public void onDisable() {
    }
    
    public SpawnProtectManager getManager() {
        return manager;
    }
}
