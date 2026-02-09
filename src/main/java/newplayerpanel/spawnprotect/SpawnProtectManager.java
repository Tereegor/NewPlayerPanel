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
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

            SpawnZone zone = loadZone(zoneName, zoneConfig);
            if (zone != null) {
                zones.put(zoneName.toLowerCase(), zone);
                plugin.getLogger().info("Spawn Protect: Loaded zone '" + zoneName + "' (" + zone.getShapeDescription() + " in " + zone.getWorldName() + ")");
            }
        }

        plugin.getLogger().info("Spawn Protect: Loaded " + zones.size() + " zone(s).");
    }

    private SpawnZone loadZone(String zoneName, ConfigurationSection zoneConfig) {
        String worldName = zoneConfig.getString("world", "world");
        String typeStr = zoneConfig.getString("type", "CIRCLE").toUpperCase();

        SpawnZone.ShapeType shapeType;
        try {
            shapeType = SpawnZone.ShapeType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Spawn Protect: Unknown shape type '" + typeStr + "' for zone '" + zoneName + "', defaulting to CIRCLE.");
            shapeType = SpawnZone.ShapeType.CIRCLE;
        }

        SpawnZone zone;
        switch (shapeType) {
            case RECT:
                double minX = zoneConfig.getDouble("min.x", 0);
                double minZ = zoneConfig.getDouble("min.z", 0);
                double maxX = zoneConfig.getDouble("max.x", 0);
                double maxZ = zoneConfig.getDouble("max.z", 0);
                zone = new SpawnZone(zoneName, worldName, minX, minZ, maxX, maxZ, true);
                break;
            case POLY:
                List<double[]> points = new ArrayList<>();
                if (zoneConfig.isList("points")) {
                    List<Map<?, ?>> pointMaps = zoneConfig.getMapList("points");
                    for (Map<?, ?> map : pointMaps) {
                        Object xObj = map.get("x");
                        Object zObj = map.get("z");
                        if (xObj instanceof Number && zObj instanceof Number) {
                            points.add(new double[]{((Number) xObj).doubleValue(), ((Number) zObj).doubleValue()});
                        }
                    }
                }
                if (points.size() < 3) {
                    plugin.getLogger().warning("Spawn Protect: Polygon zone '" + zoneName + "' needs at least 3 points, skipping.");
                    return null;
                }
                zone = new SpawnZone(zoneName, worldName, points);
                break;
            default:
                double centerX = zoneConfig.getDouble("center.x", 0);
                double centerZ = zoneConfig.getDouble("center.z", 0);
                double radius = zoneConfig.getDouble("radius", 100);
                zone = new SpawnZone(zoneName, worldName, centerX, centerZ, radius);
                break;
        }

        loadBlockBreak(zoneConfig, zone);
        loadBlockPlace(zoneConfig, zone);
        loadInteract(zoneConfig, zone);
        loadEntityInteract(zoneConfig, zone);

        zone.setPvpEnabled(zoneConfig.getBoolean("pvp.enabled", true));
        zone.setExplosionsEnabled(zoneConfig.getBoolean("explosions.enabled", true));
        zone.setFireSpreadEnabled(zoneConfig.getBoolean("fire-spread.enabled", true));

        return zone;
    }

    private void loadBlockBreak(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("block-break");
        if (section == null) { zone.setBlockBreakEnabled(false); return; }
        zone.setBlockBreakEnabled(section.getBoolean("enabled", false));
        zone.setBlockBreakWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setBlockBreakList(parseMaterials(section.getStringList("list")));
    }

    private void loadBlockPlace(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("block-place");
        if (section == null) { zone.setBlockPlaceEnabled(false); return; }
        zone.setBlockPlaceEnabled(section.getBoolean("enabled", false));
        zone.setBlockPlaceWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setBlockPlaceList(parseMaterials(section.getStringList("list")));
    }

    private void loadInteract(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("interact");
        if (section == null) { zone.setInteractEnabled(false); return; }
        zone.setInteractEnabled(section.getBoolean("enabled", false));
        zone.setInteractWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setInteractList(parseMaterials(section.getStringList("list")));
    }

    private void loadEntityInteract(ConfigurationSection zoneConfig, SpawnZone zone) {
        ConfigurationSection section = zoneConfig.getConfigurationSection("entity-interact");
        if (section == null) { zone.setEntityInteractEnabled(false); return; }
        zone.setEntityInteractEnabled(section.getBoolean("enabled", false));
        zone.setEntityInteractWhitelist(section.getString("mode", "BLACKLIST").equalsIgnoreCase("WHITELIST"));
        zone.setEntityInteractList(parseEntityTypes(section.getStringList("list")));
    }

    Set<Material> parseMaterials(List<String> list) {
        Set<Material> materials = new HashSet<>();
        for (String name : list) {
            try {
                materials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Spawn Protect: Unknown material '" + name + "'");
            }
        }
        return materials;
    }

    Set<EntityType> parseEntityTypes(List<String> list) {
        Set<EntityType> types = new HashSet<>();
        for (String name : list) {
            try {
                types.add(EntityType.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Spawn Protect: Unknown entity type '" + name + "'");
            }
        }
        return types;
    }

    public boolean saveZones() {
        if (config == null || configFile == null) return false;

        config.set("bypass-after-playtime", bypassAfterPlaytime);

        ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection != null) {
            for (String key : new ArrayList<>(zonesSection.getKeys(false))) {
                config.set("zones." + key, null);
            }
        }

        for (SpawnZone zone : zones.values()) {
            String path = "zones." + zone.getName();
            config.set(path + ".world", zone.getWorldName());
            config.set(path + ".type", zone.getShapeType().name());

            switch (zone.getShapeType()) {
                case CIRCLE:
                    config.set(path + ".center.x", zone.getCenterX());
                    config.set(path + ".center.z", zone.getCenterZ());
                    config.set(path + ".radius", zone.getRadius());
                    break;
                case RECT:
                    config.set(path + ".min.x", zone.getMinX());
                    config.set(path + ".min.z", zone.getMinZ());
                    config.set(path + ".max.x", zone.getMaxX());
                    config.set(path + ".max.z", zone.getMaxZ());
                    break;
                case POLY:
                    List<Map<String, Double>> pointsList = new ArrayList<>();
                    for (double[] p : zone.getPoints()) {
                        Map<String, Double> map = new LinkedHashMap<>();
                        map.put("x", p[0]);
                        map.put("z", p[1]);
                        pointsList.add(map);
                    }
                    config.set(path + ".points", pointsList);
                    break;
            }

            saveProtections(path, zone);
        }

        try {
            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Spawn Protect: Failed to save config: " + e.getMessage());
            return false;
        }
    }

    private void saveProtections(String path, SpawnZone zone) {
        config.set(path + ".block-break.enabled", zone.isBlockBreakEnabled());
        config.set(path + ".block-break.mode", zone.isBlockBreakWhitelist() ? "WHITELIST" : "BLACKLIST");
        config.set(path + ".block-break.list", zone.getBlockBreakList().stream().map(Enum::name).collect(Collectors.toList()));

        config.set(path + ".block-place.enabled", zone.isBlockPlaceEnabled());
        config.set(path + ".block-place.mode", zone.isBlockPlaceWhitelist() ? "WHITELIST" : "BLACKLIST");
        config.set(path + ".block-place.list", zone.getBlockPlaceList().stream().map(Enum::name).collect(Collectors.toList()));

        config.set(path + ".interact.enabled", zone.isInteractEnabled());
        config.set(path + ".interact.mode", zone.isInteractWhitelist() ? "WHITELIST" : "BLACKLIST");
        config.set(path + ".interact.list", zone.getInteractList().stream().map(Enum::name).collect(Collectors.toList()));

        config.set(path + ".entity-interact.enabled", zone.isEntityInteractEnabled());
        config.set(path + ".entity-interact.mode", zone.isEntityInteractWhitelist() ? "WHITELIST" : "BLACKLIST");
        config.set(path + ".entity-interact.list", zone.getEntityInteractList().stream().map(Enum::name).collect(Collectors.toList()));

        config.set(path + ".pvp.enabled", zone.isPvpEnabled());
        config.set(path + ".explosions.enabled", zone.isExplosionsEnabled());
        config.set(path + ".fire-spread.enabled", zone.isFireSpreadEnabled());
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
        if (!enabled || location == null) return null;
        for (SpawnZone zone : zones.values()) {
            if (zone.contains(location)) return zone;
        }
        return null;
    }

    public List<SpawnZone> getZonesAt(Location location) {
        if (!enabled || location == null) return Collections.emptyList();
        List<SpawnZone> result = new ArrayList<>();
        for (SpawnZone zone : zones.values()) {
            if (zone.contains(location)) result.add(zone);
        }
        return result;
    }

    public boolean addZone(SpawnZone zone) {
        String key = zone.getName().toLowerCase();
        if (zones.containsKey(key)) return false;
        zones.put(key, zone);
        return saveZones();
    }

    public boolean removeZone(String name) {
        if (zones.remove(name.toLowerCase()) == null) return false;
        return saveZones();
    }

    public boolean saveZone(SpawnZone zone) {
        return saveZones();
    }

    public SpawnZone createDefaultCircle(String name, String worldName, double centerX, double centerZ, double radius) {
        SpawnZone zone = new SpawnZone(name, worldName, centerX, centerZ, radius);
        applyDefaultProtections(zone);
        return zone;
    }

    public SpawnZone createDefaultRect(String name, String worldName, double minX, double minZ, double maxX, double maxZ) {
        SpawnZone zone = new SpawnZone(name, worldName, minX, minZ, maxX, maxZ, true);
        applyDefaultProtections(zone);
        return zone;
    }

    public SpawnZone createDefaultPoly(String name, String worldName, List<double[]> points) {
        SpawnZone zone = new SpawnZone(name, worldName, points);
        applyDefaultProtections(zone);
        return zone;
    }

    public SpawnZone createDefaultZone(String name, String worldName, double centerX, double centerZ, double radius) {
        return createDefaultCircle(name, worldName, centerX, centerZ, radius);
    }

    private void applyDefaultProtections(SpawnZone zone) {
        zone.setBlockBreakEnabled(true);
        zone.setBlockBreakWhitelist(true);
        zone.setBlockBreakList(Collections.emptySet());
        zone.setBlockPlaceEnabled(true);
        zone.setBlockPlaceWhitelist(true);
        zone.setBlockPlaceList(Collections.emptySet());
        zone.setInteractEnabled(true);
        zone.setInteractWhitelist(true);
        zone.setInteractList(parseMaterials(Arrays.asList("ENDER_CHEST", "CRAFTING_TABLE", "ENCHANTING_TABLE", "ANVIL",
                "CHIPPED_ANVIL", "DAMAGED_ANVIL", "SMITHING_TABLE", "GRINDSTONE", "STONECUTTER", "CARTOGRAPHY_TABLE", "LOOM")));
        zone.setEntityInteractEnabled(false);
        zone.setEntityInteractWhitelist(true);
        zone.setEntityInteractList(parseEntityTypes(Arrays.asList("VILLAGER", "WANDERING_TRADER")));
        zone.setPvpEnabled(true);
        zone.setExplosionsEnabled(true);
        zone.setFireSpreadEnabled(true);
    }

    public boolean hasPlaytimeBypass(Player player) {
        if (bypassAfterPlaytime <= 0) return false;
        return getPlayerPlaytimeSeconds(player) >= bypassAfterPlaytime;
    }

    public long getBypassAfterPlaytime() {
        return bypassAfterPlaytime;
    }

    public long getPlayerPlaytimeSeconds(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
    }

    public long getRemainingTimeForBypass(Player player) {
        if (bypassAfterPlaytime <= 0) return -1;
        return Math.max(0, bypassAfterPlaytime - getPlayerPlaytimeSeconds(player));
    }
}
