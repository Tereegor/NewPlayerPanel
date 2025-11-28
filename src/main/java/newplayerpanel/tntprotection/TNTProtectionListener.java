package newplayerpanel.tntprotection;

import newplayerpanel.restrictions.RestrictionsManager;
import newplayerpanel.util.ActionBarUtil;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TNTProtectionListener implements Listener {
    
    private final TNTProtectionManager protectionManager;
    private final RestrictionsManager restrictionsManager;
    
    public TNTProtectionListener(TNTProtectionManager protectionManager, RestrictionsManager restrictionsManager) {
        this.protectionManager = protectionManager;
        this.restrictionsManager = restrictionsManager;
    }
    
    private boolean checkTNTProtection(Player player, String restrictionKey, String restrictionName, String itemName) {
        if (player.hasPermission("newplayerpanel.tntprotection.bypass")) {
            return false;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        if (restrictionsManager != null && restrictionsManager.hasPlayerRestriction(playerUUID, restrictionName)) {
            return false;
        }
        
        int required = protectionManager.getRequiredHours(restrictionKey);
        if (required == 0 || protectionManager.canUseRestriction(playerUUID, restrictionKey)) {
            return false;
        }
        
        long current = protectionManager.getPlayerPlayTimeHours(playerUUID);
        ActionBarUtil.sendActionBar(player, "§cДля использования " + itemName + " нужно " + required + " часов. У вас: " + current + " ч");
        return true;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        protectionManager.onPlayerJoin(event.getPlayer().getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        protectionManager.removePlayer(event.getPlayer().getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.TNT) {
            return;
        }
        
        if (checkTNTProtection(event.getPlayer(), "place-tnt", "tnt_place_restriction", "TNT")) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        
        Player player = event.getPlayer();
        Material type = item.getType();
        
        if (type == Material.FLINT_AND_STEEL && checkTNTProtection(player, "use-flint-and-steel", "tnt_flint_restriction", "огнива")) {
            event.setCancelled(true);
        } else if (type == Material.FIRE_CHARGE && checkTNTProtection(player, "use-fireball", "tnt_fireball_restriction", "огненного шара")) {
            event.setCancelled(true);
        } else if (type == Material.TNT_MINECART && checkTNTProtection(player, "use-tnt-minecart", "tnt_minecart_restriction", "вагонетки с TNT")) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player) || !(event.getEntity() instanceof Fireball)) {
            return;
        }
        
        Player player = (Player) event.getEntity().getShooter();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.FIRE_CHARGE) {
            item = player.getInventory().getItemInOffHand();
            if (item.getType() != Material.FIRE_CHARGE) {
                return;
            }
        }
        
        if (checkTNTProtection(player, "use-fireball", "tnt_fireball_restriction", "огненного шара")) {
            event.setCancelled(true);
        }
    }
}
