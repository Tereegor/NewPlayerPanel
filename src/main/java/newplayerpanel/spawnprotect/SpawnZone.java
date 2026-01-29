package newplayerpanel.spawnprotect;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.Set;

public class SpawnZone {
    
    private final String name;
    private final String worldName;
    private final double centerX;
    private final double centerZ;
    private final double radius;
    
    private boolean blockBreakEnabled;
    private boolean blockBreakWhitelist;
    private Set<Material> blockBreakList;
    
    private boolean blockPlaceEnabled;
    private boolean blockPlaceWhitelist;
    private Set<Material> blockPlaceList;
    
    private boolean interactEnabled;
    private boolean interactWhitelist;
    private Set<Material> interactList;
    
    private boolean entityInteractEnabled;
    private boolean entityInteractWhitelist;
    private Set<EntityType> entityInteractList;
    
    private boolean pvpEnabled;
    private boolean explosionsEnabled;
    private boolean fireSpreadEnabled;
    
    public SpawnZone(String name, String worldName, double centerX, double centerZ, double radius) {
        this.name = name;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        
        this.blockBreakList = new HashSet<>();
        this.blockPlaceList = new HashSet<>();
        this.interactList = new HashSet<>();
        this.entityInteractList = new HashSet<>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public double getCenterX() {
        return centerX;
    }
    
    public double getCenterZ() {
        return centerZ;
    }
    
    public double getRadius() {
        return radius;
    }
    
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        double dx = location.getX() - centerX;
        double dz = location.getZ() - centerZ;
        return dx * dx + dz * dz <= radius * radius;
    }
    
    public boolean isBlockBreakEnabled() {
        return blockBreakEnabled;
    }
    
    public void setBlockBreakEnabled(boolean enabled) {
        this.blockBreakEnabled = enabled;
    }
    
    public void setBlockBreakWhitelist(boolean whitelist) {
        this.blockBreakWhitelist = whitelist;
    }
    
    public void setBlockBreakList(Set<Material> list) {
        this.blockBreakList = list != null ? list : new HashSet<>();
    }
    
    public boolean canBreakBlock(Material material) {
        if (!blockBreakEnabled) return true;
        if (blockBreakWhitelist) {
            return blockBreakList.contains(material);
        } else {
            return !blockBreakList.contains(material);
        }
    }
    
    public boolean isBlockPlaceEnabled() {
        return blockPlaceEnabled;
    }
    
    public void setBlockPlaceEnabled(boolean enabled) {
        this.blockPlaceEnabled = enabled;
    }
    
    public void setBlockPlaceWhitelist(boolean whitelist) {
        this.blockPlaceWhitelist = whitelist;
    }
    
    public void setBlockPlaceList(Set<Material> list) {
        this.blockPlaceList = list != null ? list : new HashSet<>();
    }
    
    public boolean canPlaceBlock(Material material) {
        if (!blockPlaceEnabled) return true;
        if (blockPlaceWhitelist) {
            return blockPlaceList.contains(material);
        } else {
            return !blockPlaceList.contains(material);
        }
    }
    
    public boolean isInteractEnabled() {
        return interactEnabled;
    }
    
    public void setInteractEnabled(boolean enabled) {
        this.interactEnabled = enabled;
    }
    
    public void setInteractWhitelist(boolean whitelist) {
        this.interactWhitelist = whitelist;
    }
    
    public void setInteractList(Set<Material> list) {
        this.interactList = list != null ? list : new HashSet<>();
    }
    
    public boolean canInteract(Material material) {
        if (!interactEnabled) return true;
        if (interactWhitelist) {
            return interactList.contains(material);
        } else {
            return !interactList.contains(material);
        }
    }
    
    public boolean isEntityInteractEnabled() {
        return entityInteractEnabled;
    }
    
    public void setEntityInteractEnabled(boolean enabled) {
        this.entityInteractEnabled = enabled;
    }
    
    public void setEntityInteractWhitelist(boolean whitelist) {
        this.entityInteractWhitelist = whitelist;
    }
    
    public void setEntityInteractList(Set<EntityType> list) {
        this.entityInteractList = list != null ? list : new HashSet<>();
    }
    
    public boolean canInteractEntity(EntityType type) {
        if (!entityInteractEnabled) return true;
        if (entityInteractWhitelist) {
            return entityInteractList.contains(type);
        } else {
            return !entityInteractList.contains(type);
        }
    }
    
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }
    
    public void setPvpEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
    }
    
    public boolean isExplosionsEnabled() {
        return explosionsEnabled;
    }
    
    public void setExplosionsEnabled(boolean enabled) {
        this.explosionsEnabled = enabled;
    }
    
    public boolean isFireSpreadEnabled() {
        return fireSpreadEnabled;
    }
    
    public void setFireSpreadEnabled(boolean enabled) {
        this.fireSpreadEnabled = enabled;
    }
}
