package newplayerpanel.storage;

import com.google.gson.Gson;
import newplayerpanel.restrictions.PlayerRestriction;
import newplayerpanel.villagertracker.VillagerDeathRecord;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseStorage {
    
    private final JavaPlugin plugin;
    private final Gson gson = new Gson();
    private Connection connection;
    
    public DatabaseStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        File databaseFile = new File(plugin.getDataFolder(), "database.db");
        plugin.getLogger().info("Using SQLite database: " + databaseFile.getAbsolutePath());
        
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            createTables();
            plugin.getLogger().info("Database connection established successfully.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            return false;
        }
    }
    
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            File databaseFile = new File(plugin.getDataFolder(), "database.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        }
        return connection;
    }
    
    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnDefinition) {
        try (Statement pragmaStmt = conn.createStatement();
             ResultSet rs = pragmaStmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            boolean columnExists = false;
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
            if (!columnExists) {
                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
                    plugin.getLogger().info("Added column " + columnName + " to table " + tableName);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().fine("Column " + columnName + " check/add failed: " + e.getMessage());
        }
    }
    
    private void createTables() throws SQLException {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS npp_villager_deaths (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_name TEXT NOT NULL, " +
                "player_uuid TEXT NOT NULL, " +
                "villager_type TEXT NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x REAL NOT NULL, " +
                "y REAL NOT NULL, " +
                "z REAL NOT NULL, " +
                "timestamp INTEGER NOT NULL, " +
                "enchantments TEXT, " +
                "trades TEXT, " +
                "villager_level INTEGER NOT NULL DEFAULT 1" +
                ")");
            
            addColumnIfNotExists(conn, "npp_villager_deaths", "trades", "TEXT");
            addColumnIfNotExists(conn, "npp_villager_deaths", "villager_level", "INTEGER NOT NULL DEFAULT 1");
            
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_name ON npp_villager_deaths(player_name)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON npp_villager_deaths(player_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_world_coords ON npp_villager_deaths(world, x, y, z)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_timestamp ON npp_villager_deaths(timestamp)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_timestamp ON npp_villager_deaths(player_name, timestamp)");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS npp_player_restrictions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT NOT NULL, " +
                "restriction_name TEXT NOT NULL, " +
                "expire_time INTEGER NOT NULL, " +
                "is_permanent INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER DEFAULT (strftime('%s', 'now')), " +
                "UNIQUE(player_uuid, restriction_name)" +
                ")");
            
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON npp_player_restrictions(player_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_expire_time ON npp_player_restrictions(expire_time, is_permanent)");
        }
    }
    
    public void shutdown() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Database connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    public void addVillagerDeath(VillagerDeathRecord record) {
        String sql = "INSERT INTO npp_villager_deaths (player_name, player_uuid, villager_type, world, x, y, z, timestamp, enchantments, trades, villager_level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, record.getPlayerName());
            stmt.setString(2, record.getPlayerUUID());
            stmt.setString(3, record.getVillagerType());
            stmt.setString(4, record.getWorld());
            stmt.setDouble(5, Math.round(record.getX()));
            stmt.setDouble(6, Math.round(record.getY()));
            stmt.setDouble(7, Math.round(record.getZ()));
            stmt.setLong(8, record.getTimestamp());
            
            stmt.setString(9, record.getEnchantments().isEmpty() ? null : gson.toJson(record.getEnchantments()));
            stmt.setString(10, record.getTrades().isEmpty() ? null : gson.toJson(record.getTrades()));
            stmt.setInt(11, record.getVillagerLevel());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving villager death: " + e.getMessage());
        }
    }
    
    public List<VillagerDeathRecord> getVillagerDeaths() {
        return queryVillagerDeaths("SELECT * FROM npp_villager_deaths ORDER BY timestamp DESC");
    }
    
    public List<VillagerDeathRecord> getVillagerDeathsByPlayer(String playerName) {
        return queryVillagerDeaths("SELECT * FROM npp_villager_deaths WHERE player_name = ? ORDER BY timestamp DESC", playerName);
    }
    
    public List<VillagerDeathRecord> getVillagerDeathsByCoords(double x, double y, double z, String world) {
        if (world != null) {
            return queryVillagerDeaths("SELECT * FROM npp_villager_deaths WHERE world = ? AND ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC", world, x, y, z);
        } else {
            return queryVillagerDeaths("SELECT * FROM npp_villager_deaths WHERE ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC", x, y, z);
        }
    }
    
    public List<VillagerDeathRecord> getVillagerDeathsByPlayerAndCoords(String playerName, double x, double y, double z, String world) {
        if (world != null) {
            return queryVillagerDeaths("SELECT * FROM npp_villager_deaths WHERE player_name = ? AND world = ? AND ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC", playerName, world, x, y, z);
        } else {
            return queryVillagerDeaths("SELECT * FROM npp_villager_deaths WHERE player_name = ? AND ABS(x - ?) <= 2 AND ABS(y - ?) <= 2 AND ABS(z - ?) <= 2 ORDER BY timestamp DESC", playerName, x, y, z);
        }
    }
    
    public int clearOldVillagerDeaths(long olderThanTimestamp) {
        try {
            Connection conn = getConnection();
            int deleted;
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM npp_villager_deaths WHERE timestamp < ?")) {
                stmt.setLong(1, olderThanTimestamp);
                deleted = stmt.executeUpdate();
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("VACUUM");
            } catch (SQLException ignored) {}
            return deleted;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error clearing old deaths: " + e.getMessage());
            return -1;
        }
    }
    
    private List<VillagerDeathRecord> queryVillagerDeaths(String sql, Object... params) {
        List<VillagerDeathRecord> records = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Integer> enchantments = new HashMap<>();
                String enchJson = rs.getString("enchantments");
                if (enchJson != null && !enchJson.isEmpty()) {
                    try {
                        Map<String, Double> parsed = gson.fromJson(enchJson, Map.class);
                        if (parsed != null) parsed.forEach((k, v) -> enchantments.put(k, v.intValue()));
                    } catch (Exception ignored) {}
                }
                
                List<Map<String, Object>> trades = new ArrayList<>();
                try {
                    String tradesJson = rs.getString("trades");
                    if (tradesJson != null && !tradesJson.isEmpty()) {
                        List<Map<String, Object>> parsedTrades = gson.fromJson(tradesJson, List.class);
                        if (parsedTrades != null) trades = parsedTrades;
                    }
                } catch (Exception ignored) {}
                
                int villagerLevel = 1;
                try {
                    villagerLevel = rs.getInt("villager_level");
                    if (rs.wasNull()) villagerLevel = 1;
                } catch (Exception ignored) {}
                
                records.add(new VillagerDeathRecord(
                    rs.getString("player_name"), rs.getString("player_uuid"), rs.getString("villager_type"), rs.getString("world"),
                    rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getLong("timestamp"),
                    enchantments, trades, villagerLevel
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error querying deaths: " + e.getMessage());
        }
        return records;
    }
    
    public void savePlayerRestriction(UUID playerUUID, String restrictionName, long expireTime, boolean isPermanent) {
        String sql = "INSERT OR REPLACE INTO npp_player_restrictions (player_uuid, restriction_name, expire_time, is_permanent) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, restrictionName);
            stmt.setLong(3, expireTime);
            stmt.setInt(4, isPermanent ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving restriction: " + e.getMessage());
        }
    }
    
    public void removePlayerRestriction(UUID playerUUID, String restrictionName) {
        try (PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM npp_player_restrictions WHERE player_uuid = ? AND restriction_name = ?")) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, restrictionName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error removing restriction: " + e.getMessage());
        }
    }
    
    public Map<UUID, List<PlayerRestriction>> loadPlayerRestrictions() {
        Map<UUID, List<PlayerRestriction>> restrictions = new HashMap<>();
        try (PreparedStatement stmt = getConnection().prepareStatement("SELECT * FROM npp_player_restrictions WHERE expire_time > ? OR is_permanent = 1")) {
            stmt.setLong(1, System.currentTimeMillis());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                String restrictionName = rs.getString("restriction_name");
                long expireTime = rs.getLong("expire_time");
                boolean isPermanent = rs.getInt("is_permanent") == 1;
                long durationSeconds = isPermanent ? -1 : Math.max(0, (expireTime - System.currentTimeMillis()) / 1000L);
                restrictions.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(new PlayerRestriction(playerUUID, restrictionName, durationSeconds));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading restrictions: " + e.getMessage());
        }
        return restrictions;
    }
    
    public void cleanupExpiredRestrictions() {
        long currentTime = System.currentTimeMillis();
        try (PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM npp_player_restrictions WHERE expire_time < ? AND is_permanent = 0")) {
            stmt.setLong(1, currentTime);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) plugin.getLogger().fine("Cleaned up " + deleted + " expired restrictions");
        } catch (SQLException e) {
            plugin.getLogger().warning("Error cleaning up restrictions: " + e.getMessage());
        }
    }
}
