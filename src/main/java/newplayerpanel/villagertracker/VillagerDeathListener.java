package newplayerpanel.villagertracker;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import newplayerpanel.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class VillagerDeathListener implements Listener {
    
    private final JavaPlugin plugin;
    private final VillagerDataManager dataManager;
    private final MessageManager messageManager;
    
    public VillagerDeathListener(JavaPlugin plugin, VillagerDataManager dataManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = messageManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }
        
        Villager villager = (Villager) event.getEntity();
        
        Player killer = villager.getKiller();
        if (killer == null) {
            return;
        }
        
        boolean onlyTraded = plugin.getConfig().getBoolean("villager-tracker.only-traded", true);
        
        if (onlyTraded) {
            boolean hasTraded = false;
            for (MerchantRecipe recipe : villager.getRecipes()) {
                if (recipe.getUses() > 0) {
                    hasTraded = true;
                    break;
                }
            }
            
            if (!hasTraded) {
                return;
            }
        }
        
        String villagerType;
        if (villager.getType() != org.bukkit.entity.EntityType.VILLAGER) {
            villagerType = villager.getType().getKey().getKey();
        } else {
            villagerType = villager.getProfession().getKey().getKey();
        }
        
        String world = villager.getWorld().getName();
        double x = villager.getLocation().getX();
        double y = villager.getLocation().getY();
        double z = villager.getLocation().getZ();
        
        Map<String, Integer> enchantments = new HashMap<>();
        
        for (ItemStack item : villager.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                item.getItemMeta().getEnchants().forEach((enchant, level) -> {
                    String enchantName = enchant.getKey().getKey();
                    enchantments.put(enchantName, level);
                });
            }
        }
        
        VillagerDeathRecord record = new VillagerDeathRecord(
            killer.getName(),
            killer.getUniqueId().toString(),
            villagerType,
            world,
            x, y, z,
            enchantments
        );
        
        dataManager.addRecord(record);
        
        boolean notifyEnabled = plugin.getConfig().getBoolean("villager-tracker.notify-enabled", true);
        if (notifyEnabled) {
            notifyPlayers(killer.getName(), villagerType, world, x, y, z);
        }
    }
    
    private void notifyPlayers(String killerName, String villagerType, String world, double x, double y, double z) {
        String notifyMessage = messageManager.get("tracker-notify",
            "player", killerName,
            "type", villagerType,
            "world", world
        );
        
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(notifyMessage));
        TextComponent clickableCoords = messageManager.createClickableCoords(world, x, y, z);
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("newplayerpanel.notify")) {
                onlinePlayer.spigot().sendMessage(message, clickableCoords);
            }
        }
    }
}
