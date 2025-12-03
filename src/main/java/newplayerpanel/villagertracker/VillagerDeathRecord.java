package newplayerpanel.villagertracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VillagerDeathRecord {
    
    private final String playerName;
    private final String playerUUID;
    private final String villagerType;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final long timestamp;
    private final Map<String, Integer> enchantments;
    private final List<Map<String, Object>> trades;
    private final int villagerLevel;
    
    public VillagerDeathRecord(String playerName, String playerUUID, String villagerType, String world, 
                              double x, double y, double z, Map<String, Integer> enchantments, 
                              List<Map<String, Object>> trades, int villagerLevel) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.villagerType = villagerType;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = System.currentTimeMillis();
        this.enchantments = enchantments != null ? new HashMap<>(enchantments) : new HashMap<>();
        this.trades = trades != null ? new ArrayList<>(trades) : new ArrayList<>();
        this.villagerLevel = villagerLevel;
    }
    
    public VillagerDeathRecord(String playerName, String playerUUID, String villagerType, String world, 
                              double x, double y, double z, long timestamp, Map<String, Integer> enchantments,
                              List<Map<String, Object>> trades, int villagerLevel) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.villagerType = villagerType;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.enchantments = enchantments != null ? new HashMap<>(enchantments) : new HashMap<>();
        this.trades = trades != null ? new ArrayList<>(trades) : new ArrayList<>();
        this.villagerLevel = villagerLevel;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getPlayerUUID() {
        return playerUUID;
    }
    
    public String getVillagerType() {
        return villagerType;
    }
    
    public String getWorld() {
        return world;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getZ() {
        return z;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }
    
    public List<Map<String, Object>> getTrades() {
        return new ArrayList<>(trades);
    }
    
    public int getVillagerLevel() {
        return villagerLevel;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VillagerDeathRecord{player=").append(playerName)
          .append(", type=").append(villagerType)
          .append(", world=").append(world)
          .append(", coords=").append(String.format("%.0f, %.0f, %.0f", x, y, z))
          .append(", level=").append(villagerLevel);
        
        if (!enchantments.isEmpty()) {
            sb.append(", enchantments=").append(enchantments);
        }
        
        if (!trades.isEmpty()) {
            sb.append(", trades=").append(trades.size());
        }
        
        return sb.append("}").toString();
    }
}
