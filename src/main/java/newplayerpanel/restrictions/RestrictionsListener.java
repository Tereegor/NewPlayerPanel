package newplayerpanel.restrictions;

import newplayerpanel.util.ActionBarUtil;
import newplayerpanel.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;
import java.util.UUID;

public class RestrictionsListener implements Listener {
    
    private final RestrictionsManager restrictionsManager;
    
    public RestrictionsListener(RestrictionsManager restrictionsManager) {
        this.restrictionsManager = restrictionsManager;
    }
    
    private boolean checkRestriction(Player player, Restriction restriction, String message) {
        if (player.hasPermission("newplayerpanel.restrictions.bypass")) {
            return false;
        }
        
        UUID playerUUID = player.getUniqueId();
        if (restrictionsManager.isRestricted(playerUUID, restriction)) {
            long remaining = restrictionsManager.getRestrictionRemainingTime(playerUUID, restriction.getName());
            ActionBarUtil.sendActionBar(player, "§c" + message + " Осталось: " + TimeUtil.formatTime(remaining));
            return true;
        }
        return false;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        String entityType = event.getEntityType().getKey().toString();
        
        for (Restriction restriction : restrictionsManager.getRestrictions()) {
            if (restriction.getType() == Restriction.RestrictionType.ENTITY &&
                restriction.getActions().contains("DAMAGE") &&
                restriction.getEntities().contains(entityType)) {
                
                if (checkRestriction(player, restriction, "Вы не можете наносить урон.")) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        
        Player player = event.getPlayer();
        String itemType = event.getItem().getType().getKey().toString();
        String action = event.getAction().name();
        
        for (Restriction restriction : restrictionsManager.getRestrictions()) {
            if (restriction.getType() == Restriction.RestrictionType.ITEM &&
                restriction.getItems().contains(itemType)) {
                
                Set<String> actions = restriction.getActions();
                boolean matchesAction = (actions.contains("USE") && (action.contains("RIGHT_CLICK") || action.contains("LEFT_CLICK"))) ||
                                       (actions.contains("EQUIP") && action.contains("EQUIP"));
                
                if (matchesAction && checkRestriction(player, restriction, "Использование этого предмета ограничено.")) {
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        String itemType = event.getItemDrop().getItemStack().getType().getKey().toString();
        
        for (Restriction restriction : restrictionsManager.getRestrictions()) {
            if (restriction.getType() == Restriction.RestrictionType.ITEM &&
                restriction.getActions().contains("DROP") &&
                restriction.getItems().contains(itemType)) {
                
                if (checkRestriction(player, restriction, "Вы не можете выбрасывать этот предмет.")) {
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();
        
        for (Restriction restriction : restrictionsManager.getRestrictions()) {
            if (restriction.getType() == Restriction.RestrictionType.COMMAND &&
                restriction.getActions().contains("EXECUTE") &&
                restriction.getCommands().contains(command)) {
                
                if (checkRestriction(player, restriction, "Эта команда ограничена.")) {
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }
    }
}

