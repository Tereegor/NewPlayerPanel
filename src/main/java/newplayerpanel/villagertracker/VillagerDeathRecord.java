package newplayerpanel.villagertracker;

import java.util.HashMap;
import java.util.Map;

public class VillagerDeathRecord {
    
    private final String playerName;
    private final String villagerType;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final long timestamp;
    private final Map<String, Integer> enchantments;
    
    public VillagerDeathRecord(String playerName, String villagerType, String world, 
                              double x, double y, double z, Map<String, Integer> enchantments) {
        this.playerName = playerName;
        this.villagerType = villagerType;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = System.currentTimeMillis();
        this.enchantments = enchantments != null ? new HashMap<>(enchantments) : new HashMap<>();
    }
    
    public String getPlayerName() {
        return playerName;
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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Игрок: ").append(playerName)
          .append(", Тип жителя: ").append(villagerType)
          .append(", Мир: ").append(world)
          .append(", Координаты: ").append(String.format("%.2f, %.2f, %.2f", x, y, z));
        
        if (!enchantments.isEmpty()) {
            sb.append(", Зачарования: ");
            enchantments.forEach((name, level) -> 
                sb.append(name).append(" ").append(level).append("; "));
        }
        
        return sb.toString();
    }
}

