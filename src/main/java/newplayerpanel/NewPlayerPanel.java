package newplayerpanel;

import newplayerpanel.villagertracker.VillagerTrackerModule;
import newplayerpanel.tntprotection.TNTProtectionModule;
import newplayerpanel.restrictions.RestrictionsModule;
import org.bukkit.plugin.java.JavaPlugin;

public class NewPlayerPanel extends JavaPlugin {
    
    private VillagerTrackerModule villagerTrackerModule;
    private TNTProtectionModule tntProtectionModule;
    private RestrictionsModule restrictionsModule;
    
    @Override
    public void onEnable() {
        this.villagerTrackerModule = new VillagerTrackerModule(this);
        this.restrictionsModule = new RestrictionsModule(this);
        this.tntProtectionModule = new TNTProtectionModule(this);
        this.tntProtectionModule.onEnable();
    }
    
    @Override
    public void onDisable() {
        if (villagerTrackerModule != null) {
            villagerTrackerModule.onDisable();
        }
        if (tntProtectionModule != null) {
            tntProtectionModule.onDisable();
        }
        if (restrictionsModule != null) {
            restrictionsModule.onDisable();
        }
    }
    
    public VillagerTrackerModule getVillagerTrackerModule() {
        return villagerTrackerModule;
    }
    
    public TNTProtectionModule getTNTProtectionModule() {
        return tntProtectionModule;
    }
    
    public RestrictionsModule getRestrictionsModule() {
        return restrictionsModule;
    }
}

