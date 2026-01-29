package newplayerpanel.spawnprotect;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class SpawnProtectManager {
    
    private final JavaPlugin plugin;
    private final Map<String, SpawnZone> zones;
    private File configFile;
    private FileConfiguration config;
    private boolean enabled;
    private long bypassAfterPlaytime;
    
    public SpawnProtectManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.zones = new HashMap<>();
        this.enabled = true;
    }
    
    public void loadZones() {
        zones.clear();
        
        configFile = new File(plugin.getDataFolder(), "spawnprotect.yml");
        if (!configFile.exists()) {
            plugin.saveResource("spawnprotect.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        enabled = plugin.getConfig().getBoolean("spawn-protect.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Spawn Protect: Module disabled in configuration.");
            return;
        }
        
        bypassAfterPlaytime = config.getLong("bypass-after-playtime", 0);
        
        ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection == null) {
            plugin.getLogger().info("Spawn Protect: No zones configured.");
            return;
        }
        
        for (String zoneName : zonesSection.getKeys(false)) {
            ConfigurationSection zoneConfig = zonesSection.getConfigurationSection(zoneName);
            if (zoneConfig == null) continue;
            
            String worldName = zoneConfig.getString("world", "world");
            double centerX = zoneConfig.getDouble("center.x", 0);
            double centerZ = zoneConfig.getDouble("center.z", 0);
            double radius = zoneConfig.getDouble("radius", 100);
            
            SpawnZone zone = new SpawnZone(zoneName, worldName, centerX, centerZ, radius);
            
            loadBlockBreak(zoneConfig, zone);
            loadBlockPlace(zoneConfig, zone);
            loadInteract(zoneConfig, zone);
            loadEntityInteract(zoneConfig, zone);
            
            zone.setPvpEnabled(zoneConfig.getBoolean("pvp.enabled", true));
            zone.setExplosionsEnabled(zoneConfig.getBoolean("explosions.enabled", true));
            zone.setFireSpreadEnabled(zoneConfig.getBoolean("fire-spread.enabled", true));
            
            zones.put(zoneName.toLowerCase(), zone);
            
            plugin.getLogger().info("Spawn Protect: Loaded zone '" + zoneName + "' (world: " + worldName + 
                ", center: " + centerX + ", " + centerZ + ", radius: " + radius + ")");
        }
        
        plugin.getLogger().info("Spawn Protect: Loaded " + zones.size() + " zone(s).");
    }
    
    private void loadBlockBreak(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("block-break");
        if (section == null) {
            zone.setBlockBreakEnabled(false);
            return;
        }
        
        zone.setBlockBreakEnabled(section.getBoolean("enabled", false));
        zone.setBlockBreakWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setBlockBreakList(parseMaterials(section.getStringList("list")));
    }
    
    private void loadBlockPlace(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("block-place");
        if (section == null) {
            zone.setBlockPlaceEnabled(false);
            return;
        }
        
        zone.setBlockPlaceEnabled(section.getBoolean("enabled", false));
        zone.setBlockPlaceWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setBlockPlaceList(parseMaterials(section.getStringList("list")));
    }
    
    private void loadInteract(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("interact");
        if (section == null) {
            zone.setInteractEnabled(false);
            return;
        }
        
        zone.setInteractEnabled(section.getBoolean("enabled", false));
        zone.setInteractWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setInteractList(parseMaterials(section.getStringList("list")));
    }
    
    private void loadEntityInteract(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("entity-interact");
        if (section == null) {
            zone.setEntityInteractEnabled(false);
            return;
        }
        
        zone.setEntityInteractEnabled(section.getBoolean("enabled", false));
        zone.setEntityInteractWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setEntityInteractList(parseEntityTypes(section.getStringList("list")));
    }
    
    private Set<Material> parseMaterials(List<String> list) {
        Set<Material> materials = new HashSet<>();
        for (String name : list) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                materials.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Spawn Protect: Unknown material '" + name + "'");
            }
        }
        return materials;
    }
    
    private Set<EntityType> parseEntityTypes(List<String> list) {
        Set<EntityType> types = new HashSet<>();
        for (String name : list) {
            try {
                EntityType type = EntityType.valueOf(name.toUpperCase());
                types.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Spawn Protect: Unknown entity type '" + name + "'");
            }
        }
        return types;
    }
    
    public void reload() {
        loadZones();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Collection<SpawnZone> getZones() {
        return Collections.unmodifiableCollection(zones.values());
    }
    
    public SpawnZone getZone(String name) {
        return zones.get(name.toLowerCase());
    }
    
    public SpawnZone getZoneAt(Location location) {
        if (!enabled || location == null) {
            return null;
        }
        
        for (SpawnZone zone : zones.values()) {
            if (zone.contains(location)) {
                return zone;
            }
        }
        return null;
    }
    
    public List<SpawnZone> getZonesAt(Location location) {
        if (!enabled || location == null) {
            return Collections.emptyList();
        }
        
        List<SpawnZone> result = new ArrayList<>();
        for (SpawnZone zone : zones.values()) {
            if (zone.contains(location)) {
                result.add(zone);
            }
        }
        return result;
    }
    
    public boolean hasPlaytimeBypass(Player player) {
        if (bypassAfterPlaytime <= 0) {
            return false;
        }
        long playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long playTimeSeconds = playTimeTicks / 20;
        return playTimeSeconds >= bypassAfterPlaytime;
    }
    
    public long getBypassAfterPlaytime() {
        return bypassAfterPlaytime;
    }
    
    public long getPlayerPlaytimeSeconds(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
    }
    
    public long getRemainingTimeForBypass(Player player) {
        if (bypassAfterPlaytime <= 0) {
            return -1;
        }
        long playTimeSeconds = getPlayerPlaytimeSeconds(player);
        return Math.max(0, bypassAfterPlaytime - playTimeSeconds);
    }
}
