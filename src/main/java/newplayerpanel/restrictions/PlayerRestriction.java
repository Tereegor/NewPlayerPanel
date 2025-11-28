package newplayerpanel.restrictions;

import java.util.UUID;

public class PlayerRestriction {
    
    private final UUID playerUUID;
    private final String restrictionName;
    private final long expireTime;
    private final long durationSeconds;
    private final boolean isPermanent;
    
    public PlayerRestriction(UUID playerUUID, String restrictionName, long durationSeconds) {
        this.playerUUID = playerUUID;
        this.restrictionName = restrictionName;
        this.durationSeconds = durationSeconds;
        this.isPermanent = durationSeconds == -1;
        this.expireTime = isPermanent ? Long.MAX_VALUE : System.currentTimeMillis() + (durationSeconds * 1000L);
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getRestrictionName() {
        return restrictionName;
    }
    
    public long getDurationSeconds() {
        return durationSeconds;
    }
    
    public boolean isPermanent() {
        return isPermanent;
    }
    
    public boolean isExpired() {
        if (isPermanent) {
            return false;
        }
        return System.currentTimeMillis() >= expireTime;
    }
    
    public long getRemainingSeconds() {
        if (isPermanent) {
            return -1;
        }
        long remaining = (expireTime - System.currentTimeMillis()) / 1000L;
        return remaining > 0 ? remaining : 0;
    }
}

