package newplayerpanel.spawnprotect;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;

public class SpawnZone {

    public enum ShapeType {
        CIRCLE, RECT, POLY
    }

    private final String name;
    private String worldName;

    private ShapeType shapeType;

    private double centerX;
    private double centerZ;
    private double radius;

    private double minX, minZ, maxX, maxZ;

    private List<double[]> points;

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
        this.shapeType = ShapeType.CIRCLE;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.points = new ArrayList<>();
        initLists();
    }

    public SpawnZone(String name, String worldName, double minX, double minZ, double maxX, double maxZ, boolean rect) {
        this.name = name;
        this.worldName = worldName;
        this.shapeType = ShapeType.RECT;
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        this.centerX = (this.minX + this.maxX) / 2.0;
        this.centerZ = (this.minZ + this.maxZ) / 2.0;
        this.points = new ArrayList<>();
        initLists();
    }

    public SpawnZone(String name, String worldName, List<double[]> points) {
        this.name = name;
        this.worldName = worldName;
        this.shapeType = ShapeType.POLY;
        this.points = points != null ? new ArrayList<>(points) : new ArrayList<>();
        recalcPolyCenter();
        initLists();
    }

    private void initLists() {
        this.blockBreakList = new HashSet<>();
        this.blockPlaceList = new HashSet<>();
        this.interactList = new HashSet<>();
        this.entityInteractList = new HashSet<>();
    }

    private void recalcPolyCenter() {
        if (points == null || points.isEmpty()) {
            this.centerX = 0;
            this.centerZ = 0;
            return;
        }
        double sumX = 0, sumZ = 0;
        for (double[] p : points) {
            sumX += p[0];
            sumZ += p[1];
        }
        this.centerX = sumX / points.size();
        this.centerZ = sumZ / points.size();
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
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

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public void setCenterZ(double centerZ) {
        this.centerZ = centerZ;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getMinX() { return minX; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxZ() { return maxZ; }

    public void setRect(double minX, double minZ, double maxX, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        this.centerX = (this.minX + this.maxX) / 2.0;
        this.centerZ = (this.minZ + this.maxZ) / 2.0;
    }

    public List<double[]> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public void setPoints(List<double[]> points) {
        this.points = points != null ? new ArrayList<>(points) : new ArrayList<>();
        recalcPolyCenter();
    }

    public void addPoint(double x, double z) {
        this.points.add(new double[]{x, z});
        recalcPolyCenter();
    }

    public boolean removePoint(int index) {
        if (index < 0 || index >= points.size()) return false;
        points.remove(index);
        recalcPolyCenter();
        return true;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }

        double x = location.getX();
        double z = location.getZ();

        switch (shapeType) {
            case CIRCLE:
                return containsCircle(x, z);
            case RECT:
                return containsRect(x, z);
            case POLY:
                return containsPoly(x, z);
            default:
                return false;
        }
    }

    private boolean containsCircle(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz <= radius * radius;
    }

    private boolean containsRect(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private boolean containsPoly(double x, double z) {
        if (points.size() < 3) return false;
        boolean inside = false;
        int n = points.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i)[0], zi = points.get(i)[1];
            double xj = points.get(j)[0], zj = points.get(j)[1];
            if (zi == zj) continue;
            if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    public String getShapeDescription() {
        switch (shapeType) {
            case CIRCLE:
                return String.format("circle(%.0f, %.0f, r=%.0f)", centerX, centerZ, radius);
            case RECT:
                return String.format("rect(%.0f,%.0f -> %.0f,%.0f)", minX, minZ, maxX, maxZ);
            case POLY:
                return String.format("poly(%d pts)", points.size());
            default:
                return "unknown";
        }
    }

    public boolean isBlockBreakEnabled() { return blockBreakEnabled; }
    public void setBlockBreakEnabled(boolean enabled) { this.blockBreakEnabled = enabled; }
    public void setBlockBreakWhitelist(boolean whitelist) { this.blockBreakWhitelist = whitelist; }
    public boolean isBlockBreakWhitelist() { return blockBreakWhitelist; }
    public void setBlockBreakList(Set<Material> list) { this.blockBreakList = list != null ? list : new HashSet<>(); }
    public Set<Material> getBlockBreakList() { return blockBreakList; }
    public boolean canBreakBlock(Material material) {
        if (!blockBreakEnabled) return true;
        return blockBreakWhitelist ? blockBreakList.contains(material) : !blockBreakList.contains(material);
    }

    public boolean isBlockPlaceEnabled() { return blockPlaceEnabled; }
    public void setBlockPlaceEnabled(boolean enabled) { this.blockPlaceEnabled = enabled; }
    public void setBlockPlaceWhitelist(boolean whitelist) { this.blockPlaceWhitelist = whitelist; }
    public boolean isBlockPlaceWhitelist() { return blockPlaceWhitelist; }
    public void setBlockPlaceList(Set<Material> list) { this.blockPlaceList = list != null ? list : new HashSet<>(); }
    public Set<Material> getBlockPlaceList() { return blockPlaceList; }
    public boolean canPlaceBlock(Material material) {
        if (!blockPlaceEnabled) return true;
        return blockPlaceWhitelist ? blockPlaceList.contains(material) : !blockPlaceList.contains(material);
    }

    public boolean isInteractEnabled() { return interactEnabled; }
    public void setInteractEnabled(boolean enabled) { this.interactEnabled = enabled; }
    public void setInteractWhitelist(boolean whitelist) { this.interactWhitelist = whitelist; }
    public boolean isInteractWhitelist() { return interactWhitelist; }
    public void setInteractList(Set<Material> list) { this.interactList = list != null ? list : new HashSet<>(); }
    public Set<Material> getInteractList() { return interactList; }
    public boolean canInteract(Material material) {
        if (!interactEnabled) return true;
        return interactWhitelist ? interactList.contains(material) : !interactList.contains(material);
    }

    public boolean isEntityInteractEnabled() { return entityInteractEnabled; }
    public void setEntityInteractEnabled(boolean enabled) { this.entityInteractEnabled = enabled; }
    public void setEntityInteractWhitelist(boolean whitelist) { this.entityInteractWhitelist = whitelist; }
    public boolean isEntityInteractWhitelist() { return entityInteractWhitelist; }
    public void setEntityInteractList(Set<EntityType> list) { this.entityInteractList = list != null ? list : new HashSet<>(); }
    public Set<EntityType> getEntityInteractList() { return entityInteractList; }
    public boolean canInteractEntity(EntityType type) {
        if (!entityInteractEnabled) return true;
        return entityInteractWhitelist ? entityInteractList.contains(type) : !entityInteractList.contains(type);
    }

    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean enabled) { this.pvpEnabled = enabled; }

    public boolean isExplosionsEnabled() { return explosionsEnabled; }
    public void setExplosionsEnabled(boolean enabled) { this.explosionsEnabled = enabled; }

    public boolean isFireSpreadEnabled() { return fireSpreadEnabled; }
    public void setFireSpreadEnabled(boolean enabled) { this.fireSpreadEnabled = enabled; }
}
