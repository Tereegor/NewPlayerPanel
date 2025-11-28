package newplayerpanel.restrictions;

public class DefaultRestrictionState {
    
    private final String restrictionName;
    private final long expireTime;
    private final boolean isPermanent;
    
    public DefaultRestrictionState(String restrictionName, long durationSeconds) {
        this.restrictionName = restrictionName;
        this.isPermanent = durationSeconds == -1;
        this.expireTime = isPermanent ? Long.MAX_VALUE : System.currentTimeMillis() + (durationSeconds * 1000L);
    }
    
    public boolean isExpired() {
        if (isPermanent) {
            return false;
        }
        return System.currentTimeMillis() >= expireTime;
    }
    
    public boolean isPermanent() {
        return isPermanent;
    }
    
    public String getRestrictionName() {
        return restrictionName;
    }
    
    public long getRemainingSeconds() {
        if (isPermanent) {
            return -1;
        }
        long remaining = (expireTime - System.currentTimeMillis()) / 1000L;
        return remaining > 0 ? remaining : 0;
    }
}

