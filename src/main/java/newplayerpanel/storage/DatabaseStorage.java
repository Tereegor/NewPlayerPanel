package newplayerpanel.storage;

import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import newplayerpanel.restrictions.PlayerRestriction;
import newplayerpanel.villagertracker.VillagerDeathRecord;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseStorage implements StorageProvider {
    
    private final JavaPlugin plugin;
    private final String storageType;
    private HikariDataSource dataSource;
    private final Gson gson = new Gson();
    private boolean isMySQL = false;
    
    public DatabaseStorage(JavaPlugin plugin, String storageType) {
        this.plugin = plugin;
        this.storageType = storageType;
    }
    
    @Override
    public boolean initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        HikariConfig hikariConfig = new HikariConfig();
        
        if (storageType.equals("MYSQL") || storageType.equals("MARIADB")) {
            isMySQL = true;
            FileConfiguration config = plugin.getConfig();
            
            String host = config.getString("database.host", "localhost");
            int port = config.getInt("database.port", 3306);
            String database = config.getString("database.database", "newplayerpanel");
            String username = config.getString("database.username", "root");
            String password = config.getString("database.password", "");
            
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true";
            
            if (storageType.equals("MARIADB")) {
                jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database + 
                    "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4&useUnicode=true";
                hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
            } else {
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
            
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maximum-pool-size", 10));
            hikariConfig.setMinimumIdle(config.getInt("database.pool.minimum-idle", 2));
            hikariConfig.setConnectionTimeout(config.getLong("database.pool.connection-timeout", 30000));
            hikariConfig.setIdleTimeout(config.getLong("database.pool.idle-timeout", 600000));
            hikariConfig.setMaxLifetime(config.getLong("database.pool.max-lifetime", 1800000));
            
            plugin.getLogger().info("Connecting to " + storageType + " database: " + host + ":" + port + "/" + database);
        } else {
            File databaseFile = new File(plugin.getDataFolder(), "database.db");
            plugin.getLogger().info("Using H2/SQLite database: " + databaseFile.getAbsolutePath());
            
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(30000);
        }
        
        try {
            dataSource = new HikariDataSource(hikariConfig);
            createTables();
            plugin.getLogger().info("Database connection established successfully.");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            plugin.getLogger().severe("SQL State: " + e.getSQLState());
            plugin.getLogger().severe("Error Code: " + e.getErrorCode());
            if (e.getCause() != null) {
                plugin.getLogger().severe("Cause: " + e.getCause().getMessage());
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            plugin.getLogger().severe("Exception type: " + e.getClass().getName());
            if (e.getCause() != null) {
                plugin.getLogger().severe("Cause: " + e.getCause().getMessage());
            }
            return false;
        }
    }
    
    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnDefinition) {
        try {
            if (isMySQL) {
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                    checkStmt.setString(1, tableName);
                    checkStmt.setString(2, columnName);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            try (Statement alterStmt = conn.createStatement()) {
                                alterStmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
                                plugin.getLogger().info("Added column " + columnName + " to table " + tableName);
                            }
                        }
                    }
                }
            } else {
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
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().fine("Column " + columnName + " check/add failed: " + e.getMessage());
        }
    }
    
    private void createTables() throws SQLException {
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                if (isMySQL) {
                    stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS npp_villager_deaths (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_name VARCHAR(64) NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL,
                        villager_type VARCHAR(128) NOT NULL,
                        world VARCHAR(128) NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        timestamp BIGINT NOT NULL,
                        enchantments TEXT,
                        trades TEXT,
                        villager_level INT NOT NULL DEFAULT 1,
                        INDEX idx_player_name (player_name),
                        INDEX idx_player_uuid (player_uuid),
                        INDEX idx_world_coords (world, x, y, z),
                        INDEX idx_timestamp (timestamp),
                        INDEX idx_player_timestamp (player_name, timestamp)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
                
                addColumnIfNotExists(conn, "npp_villager_deaths", "trades", "TEXT");
                addColumnIfNotExists(conn, "npp_villager_deaths", "villager_level", "INT NOT NULL DEFAULT 1");
                
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS npp_player_restrictions (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        restriction_name VARCHAR(128) NOT NULL,
                        expire_time BIGINT NOT NULL,
                        is_permanent TINYINT(1) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY unique_player_restriction (player_uuid, restriction_name),
                        INDEX idx_player_uuid (player_uuid),
                        INDEX idx_expire_time (expire_time, is_permanent)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            } else {
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
                        enchantments TEXT,
                        trades TEXT,
                        villager_level INTEGER NOT NULL DEFAULT 1
                    )
                """);
                
                addColumnIfNotExists(conn, "npp_villager_deaths", "trades", "TEXT");
                addColumnIfNotExists(conn, "npp_villager_deaths", "villager_level", "INTEGER NOT NULL DEFAULT 1");
                
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_name ON npp_villager_deaths(player_name)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON npp_villager_deaths(player_uuid)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_world_coords ON npp_villager_deaths(world, x, y, z)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_timestamp ON npp_villager_deaths(timestamp)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_timestamp ON npp_villager_deaths(player_name, timestamp)");
                
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS npp_player_restrictions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        restriction_name TEXT NOT NULL,
                        expire_time INTEGER NOT NULL,
                        is_permanent INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER DEFAULT (strftime('%s', 'now')),
                        UNIQUE(player_uuid, restriction_name)
                    )
                """);
                
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON npp_player_restrictions(player_uuid)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_expire_time ON npp_player_restrictions(expire_time, is_permanent)");
            }
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
        return false;
    }
    
    @Override
    public void saveMessage(String language, String key, String value) {
    }
    
    @Override
    public Map<String, String> loadMessages(String language) {
        return new HashMap<>();
    }
    
    @Override
    public void addVillagerDeath(VillagerDeathRecord record) {
        String sql = "INSERT INTO npp_villager_deaths (player_name, player_uuid, villager_type, world, x, y, z, timestamp, enchantments, trades, villager_level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getPlayerName());
            stmt.setString(2, record.getPlayerUUID());
            stmt.setString(3, record.getVillagerType());
            stmt.setString(4, record.getWorld());
            stmt.setDouble(5, Math.round(record.getX()));
            stmt.setDouble(6, Math.round(record.getY()));
            stmt.setDouble(7, Math.round(record.getZ()));
            stmt.setLong(8, record.getTimestamp());
            
            String enchantmentsJson = record.getEnchantments().isEmpty() ? null : gson.toJson(record.getEnchantments());
            stmt.setString(9, enchantmentsJson);
            
            String tradesJson = record.getTrades().isEmpty() ? null : gson.toJson(record.getTrades());
            stmt.setString(10, tradesJson);
            
            stmt.setInt(11, record.getVillagerLevel());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving villager death: " + e.getMessage());
            plugin.getLogger().fine("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
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
            int deleted;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM npp_villager_deaths WHERE timestamp < ?")) {
                stmt.setLong(1, olderThanTimestamp);
                deleted = stmt.executeUpdate();
            }
            
            if (!isMySQL) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("VACUUM");
                } catch (SQLException e) {
                }
            }
            
            return deleted;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error clearing old villager deaths: " + e.getMessage());
            plugin.getLogger().fine("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
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
                
                List<Map<String, Object>> trades = new ArrayList<>();
                try {
                    String tradesJson = rs.getString("trades");
                    if (tradesJson != null && !tradesJson.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> parsedTrades = gson.fromJson(tradesJson, List.class);
                        if (parsedTrades != null) {
                            trades = parsedTrades;
                        }
                    }
                } catch (Exception ignored) {}
                
                int villagerLevel = 1;
                try {
                    villagerLevel = rs.getInt("villager_level");
                    if (rs.wasNull()) {
                        villagerLevel = 1;
                    }
                } catch (Exception ignored) {}
                
                records.add(new VillagerDeathRecord(
                    rs.getString("player_name"),
                    rs.getString("player_uuid"),
                    rs.getString("villager_type"),
                    rs.getString("world"),
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getLong("timestamp"),
                    enchantments,
                    trades,
                    villagerLevel
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error querying villager deaths: " + e.getMessage());
        }
        return records;
    }
    
    @Override
    public void savePlayerRestriction(UUID playerUUID, String restrictionName, long expireTime, boolean isPermanent) {
        String sql = isMySQL 
            ? "INSERT INTO npp_player_restrictions (player_uuid, restriction_name, expire_time, is_permanent) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE expire_time = VALUES(expire_time), is_permanent = VALUES(is_permanent)"
            : "INSERT OR REPLACE INTO npp_player_restrictions (player_uuid, restriction_name, expire_time, is_permanent) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        long currentTime = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM npp_player_restrictions WHERE expire_time < ? AND is_permanent = 0")) {
            stmt.setLong(1, currentTime);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                plugin.getLogger().fine("Cleaned up " + deleted + " expired player restrictions");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error cleaning up restrictions: " + e.getMessage());
            plugin.getLogger().fine("Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
        }
    }
}
