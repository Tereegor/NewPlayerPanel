package newplayerpanel.storage;

import newplayerpanel.restrictions.PlayerRestriction;
import newplayerpanel.villagertracker.VillagerDeathRecord;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StorageProvider {
    boolean initialize();
    void shutdown();
    boolean messagesExist(String language);
    void saveMessage(String language, String key, String value);
    Map<String, String> loadMessages(String language);
    void addVillagerDeath(VillagerDeathRecord record);
    List<VillagerDeathRecord> getVillagerDeaths();
    List<VillagerDeathRecord> getVillagerDeathsByPlayer(String playerName);
    List<VillagerDeathRecord> getVillagerDeathsByCoords(double x, double y, double z, String world);
    List<VillagerDeathRecord> getVillagerDeathsByPlayerAndCoords(String playerName, double x, double y, double z, String world);
    int clearOldVillagerDeaths(long olderThanTimestamp);
    void savePlayerRestriction(UUID playerUUID, String restrictionName, long expireTime, boolean isPermanent);
    void removePlayerRestriction(UUID playerUUID, String restrictionName);
    Map<UUID, List<PlayerRestriction>> loadPlayerRestrictions();
    void cleanupExpiredRestrictions();
}
