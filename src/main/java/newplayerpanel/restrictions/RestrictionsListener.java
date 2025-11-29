package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.ActionBarUtil;
import newplayerpanel.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public class RestrictionsListener implements Listener {
    
    private final RestrictionsManager restrictionsManager;
    private final MessageManager messageManager;
    
    public RestrictionsListener(RestrictionsManager restrictionsManager, MessageManager messageManager) {
        this.restrictionsManager = restrictionsManager;
        this.messageManager = messageManager;
    }
    
    private boolean checkRestriction(Player player, Restriction restriction, String messageKey) {
        if (player.hasPermission("newplayerpanel.restrictions.bypass")) {
            return false;
        }
        
        UUID playerUUID = player.getUniqueId();
        if (restrictionsManager.isRestricted(playerUUID, restriction)) {
            long remaining = restrictionsManager.getRestrictionRemainingTime(playerUUID, restriction.getName());
            String timeFormatted = TimeUtil.formatTimeLocalized(remaining, messageManager);
            String message = messageManager.get(messageKey, "time", timeFormatted);
            ActionBarUtil.sendActionBar(player, message);
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
                
                if (checkRestriction(player, restriction, "restrictions-blocked-damage")) {
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
                
                if (matchesAction && checkRestriction(player, restriction, "restrictions-blocked-item")) {
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
                
                if (checkRestriction(player, restriction, "restrictions-blocked-drop")) {
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
                
                if (checkRestriction(player, restriction, "restrictions-blocked-command")) {
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        boolean isEquipSlot = event.getSlotType() == InventoryType.SlotType.ARMOR ||
                             (event.getRawSlot() >= 36 && event.getRawSlot() <= 39);
        
        ItemStack itemToCheck = null;
        
        if (isEquipSlot && cursorItem != null && cursorItem.getType() != org.bukkit.Material.AIR) {
            itemToCheck = cursorItem;
        } else if (event.isShiftClick() && clickedItem != null && clickedItem.getType() != org.bukkit.Material.AIR) {
            String itemName = clickedItem.getType().name().toLowerCase();
            if (itemName.contains("helmet") || itemName.contains("chestplate") || 
                itemName.contains("leggings") || itemName.contains("boots") || 
                itemName.contains("elytra") || itemName.contains("cap") || itemName.contains("head")) {
                itemToCheck = clickedItem;
            }
        } else if (event.getClick().name().contains("NUMBER") && clickedItem != null) {
            itemToCheck = clickedItem;
        }
        
        if (itemToCheck == null) {
            return;
        }
        
        String itemType = itemToCheck.getType().getKey().toString();
        
        for (Restriction restriction : restrictionsManager.getRestrictions()) {
            if (restriction.getType() == Restriction.RestrictionType.ITEM &&
                restriction.getActions().contains("EQUIP") &&
                restriction.getItems().contains(itemType)) {
                
                if (checkRestriction(player, restriction, "restrictions-blocked-item")) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
