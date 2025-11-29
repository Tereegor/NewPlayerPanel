package newplayerpanel.storage;

import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import newplayerpanel.restrictions.PlayerRestriction;
import newplayerpanel.villagertracker.VillagerDeathRecord;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseStorage implements StorageProvider {
    
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private final Gson gson = new Gson();
    
    public DatabaseStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        File databaseFile = new File(plugin.getDataFolder(), "database.db");
        plugin.getLogger().info("Using SQLite database: " + databaseFile.getAbsolutePath());
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);
        
        try {
            dataSource = new HikariDataSource(hikariConfig);
            createTables();
            plugin.getLogger().info("Database connection established successfully.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS npp_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    language TEXT NOT NULL,
                    message_key TEXT NOT NULL,
                    message_value TEXT NOT NULL,
                    UNIQUE(language, message_key)
                )
            """);
            
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS npp_villager_deaths (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    villager_type TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    timestamp INTEGER NOT NULL,
                    enchantments TEXT
                )
            """);
            
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player ON npp_villager_deaths(player_name)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_coords ON npp_villager_deaths(world, x, y, z)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_timestamp ON npp_villager_deaths(timestamp)");
            
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS npp_player_restrictions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    restriction_name TEXT NOT NULL,
                    expire_time INTEGER NOT NULL,
                    is_permanent INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(player_uuid, restriction_name)
                )
            """);
            
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON npp_player_restrictions(player_uuid)");
        }
    }
    
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed.");
        }
    }
    
    @Override
    public boolean messagesExist(String language) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM npp_messages WHERE language = ?")) {
            stmt.setString(1, language);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking messages: " + e.getMessage());
        }
        return false;
    }
    
    @Override
    public void saveMessage(String language, String key, String value) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO npp_messages (language, message_key, message_value) VALUES (?, ?, ?)")) {
            stmt.setString(1, language);
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving message: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, String> loadMessages(String language) {
        Map<String, String> messages = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT message_key, message_value FROM npp_messages WHERE language = ?")) {
            stmt.setString(1, language);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.put(rs.getString("message_key"), rs.getString("message_value"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading messages: " + e.getMessage());
        }
        return messages;
    }
    
    @Override
    public void addVillagerDeath(VillagerDeathRecord record) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO npp_villager_deaths (player_name, player_uuid, villager_type, world, x, y, z, timestamp, enchantments) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, record.getPlayerName());
            stmt.setString(2, record.getPlayerUUID());
            stmt.setString(3, record.getVillagerType());
            stmt.setString(4, record.getWorld());
            stmt.setDouble(5, record.getX());
            stmt.setDouble(6, record.getY());
            stmt.setDouble(7, record.getZ());
            stmt.setLong(8, record.getTimestamp());
            stmt.setString(9, gson.toJson(record.getEnchantments()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving villager death: " + e.getMessage());
        }
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeaths() {
        return queryVillagerDeaths("SELECT * FROM npp_villager_deaths ORDER BY timestamp DESC");
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeathsByPlayer(String playerName) {
        return queryVillagerDeaths(
            "SELECT * FROM npp_villager_deaths WHERE player_name = ? ORDER BY timestamp DESC",
            playerName);
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeathsByCoords(double x, double y, double z, String world) {
        if (world != null) {
            return queryVillagerDeaths(
                "SELECT * FROM npp_villager_deaths WHERE world = ? AND ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC",
                world, x, y, z);
        } else {
            return queryVillagerDeaths(
                "SELECT * FROM npp_villager_deaths WHERE ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC",
                x, y, z);
        }
    }
    
    @Override
    public List<VillagerDeathRecord> getVillagerDeathsByPlayerAndCoords(String playerName, double x, double y, double z, String world) {
        if (world != null) {
            return queryVillagerDeaths(
                "SELECT * FROM npp_villager_deaths WHERE player_name = ? AND world = ? AND ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC",
                playerName, world, x, y, z);
        } else {
            return queryVillagerDeaths(
                "SELECT * FROM npp_villager_deaths WHERE player_name = ? AND ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC",
                playerName, x, y, z);
        }
    }
    
    @Override
    public int clearOldVillagerDeaths(long olderThanTimestamp) {
        try (Connection conn = dataSource.getConnection()) {
            int count;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM npp_villager_deaths WHERE timestamp < ?")) {
                stmt.setLong(1, olderThanTimestamp);
                ResultSet rs = stmt.executeQuery();
                count = rs.next() ? rs.getInt(1) : 0;
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM npp_villager_deaths WHERE timestamp < ?")) {
                stmt.setLong(1, olderThanTimestamp);
                stmt.executeUpdate();
            }
            return count;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error clearing old villager deaths: " + e.getMessage());
            return -1;
        }
    }
    
    private List<VillagerDeathRecord> queryVillagerDeaths(String sql, Object... params) {
        List<VillagerDeathRecord> records = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Integer> enchantments = new HashMap<>();
                String enchJson = rs.getString("enchantments");
                if (enchJson != null && !enchJson.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Double> parsed = gson.fromJson(enchJson, Map.class);
                        if (parsed != null) {
                            parsed.forEach((k, v) -> enchantments.put(k, v.intValue()));
                        }
                    } catch (Exception ignored) {}
                }
                records.add(new VillagerDeathRecord(
                    rs.getString("player_name"),
                    rs.getString("player_uuid"),
                    rs.getString("villager_type"),
                    rs.getString("world"),
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getLong("timestamp"),
                    enchantments
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error querying villager deaths: " + e.getMessage());
        }
        return records;
    }
    
    @Override
    public void savePlayerRestriction(UUID playerUUID, String restrictionName, long expireTime, boolean isPermanent) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO npp_player_restrictions (player_uuid, restriction_name, expire_time, is_permanent) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, restrictionName);
            stmt.setLong(3, expireTime);
            stmt.setInt(4, isPermanent ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving player restriction: " + e.getMessage());
        }
    }
    
    @Override
    public void removePlayerRestriction(UUID playerUUID, String restrictionName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM npp_player_restrictions WHERE player_uuid = ? AND restriction_name = ?")) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, restrictionName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error removing player restriction: " + e.getMessage());
        }
    }
    
    @Override
    public Map<UUID, List<PlayerRestriction>> loadPlayerRestrictions() {
        Map<UUID, List<PlayerRestriction>> restrictions = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM npp_player_restrictions WHERE expire_time > ? OR is_permanent = 1")) {
            stmt.setLong(1, System.currentTimeMillis());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                String restrictionName = rs.getString("restriction_name");
                long expireTime = rs.getLong("expire_time");
                boolean isPermanent = rs.getInt("is_permanent") == 1;
                
                long durationSeconds = isPermanent ? -1 : Math.max(0, (expireTime - System.currentTimeMillis()) / 1000L);
                
                restrictions.computeIfAbsent(playerUUID, k -> new ArrayList<>())
                    .add(new PlayerRestriction(playerUUID, restrictionName, durationSeconds));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading player restrictions: " + e.getMessage());
        }
        return restrictions;
    }
    
    @Override
    public void cleanupExpiredRestrictions() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM npp_player_restrictions WHERE expire_time < ? AND is_permanent = 0")) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error cleaning up restrictions: " + e.getMessage());
        }
    }
}
