package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.ActionBarUtil;
import newplayerpanel.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class RestrictionsListener implements Listener {
    
    private final RestrictionsManager restrictionsManager;
    private final MessageManager messageManager;
    private final org.bukkit.plugin.Plugin plugin;
    private int armorCheckTaskId = -1;
    
    public RestrictionsListener(RestrictionsManager restrictionsManager, MessageManager messageManager, org.bukkit.plugin.Plugin plugin) {
        this.restrictionsManager = restrictionsManager;
        this.messageManager = messageManager;
        this.plugin = plugin;
        startArmorCheckTask();
    }
    
    private void startArmorCheckTask() {
        if (plugin == null) {
            return;
        }
        
        armorCheckTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayerArmorSlots(player);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L).getTaskId();
    }
    
    public void stopArmorCheckTask() {
        if (armorCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(armorCheckTaskId);
            armorCheckTaskId = -1;
        }
    }
    
    private void checkPlayerArmorSlots(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerInventory inventory = player.getInventory();
        
        ItemStack helmet = inventory.getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR && isRestrictedForEquip(player, helmet)) {
            ItemStack itemToMove = helmet.clone();
            inventory.setHelmet(null);
            moveItemToInventoryOrDrop(player, itemToMove);
        }
        
        ItemStack chestplate = inventory.getChestplate();
        if (chestplate != null && chestplate.getType() != Material.AIR && isRestrictedForEquip(player, chestplate)) {
            ItemStack itemToMove = chestplate.clone();
            inventory.setChestplate(null);
            moveItemToInventoryOrDrop(player, itemToMove);
        }
        
        ItemStack leggings = inventory.getLeggings();
        if (leggings != null && leggings.getType() != Material.AIR && isRestrictedForEquip(player, leggings)) {
            ItemStack itemToMove = leggings.clone();
            inventory.setLeggings(null);
            moveItemToInventoryOrDrop(player, itemToMove);
        }
        
        ItemStack boots = inventory.getBoots();
        if (boots != null && boots.getType() != Material.AIR && isRestrictedForEquip(player, boots)) {
            ItemStack itemToMove = boots.clone();
            inventory.setBoots(null);
            moveItemToInventoryOrDrop(player, itemToMove);
        }
    }
    
    private boolean checkRestriction(Player player, Restriction restriction, String messageKey) {
        if (player.hasPermission("newplayerpanel.restrictions.bypass")) {
            return false;
        }
        
        if (restrictionsManager.isRestricted(player.getUniqueId(), restriction)) {
            long remaining = restrictionsManager.getRestrictionRemainingTime(player.getUniqueId(), restriction.getName());
            String timeFormatted = TimeUtil.formatTimeLocalized(remaining, messageManager);
            String message = messageManager.get(messageKey, "time", timeFormatted);
            ActionBarUtil.sendActionBar(player, message);
            return true;
        }
        return false;
    }
    
    private void moveItemToInventoryOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return;
        }
        
        PlayerInventory inventory = player.getInventory();
        int emptySlot = inventory.firstEmpty();
        
        if (emptySlot != -1) {
            inventory.setItem(emptySlot, item.clone());
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
        }
    }
    
    private boolean isRestrictedForEquip(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        String itemType = item.getType().getKey().toString();
        
        for (Restriction restriction : restrictionsManager.getRestrictions()) {
            if ((restriction.getType() == Restriction.RestrictionType.EQUIPMENT || 
                 restriction.getType() == Restriction.RestrictionType.ITEM) &&
                restriction.getActions().contains("EQUIP") &&
                restriction.getItems().contains(itemType)) {
                
                return checkRestriction(player, restriction, "restrictions-blocked-item");
            }
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
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        Player player = event.getPlayer();
        String itemType = item.getType().getKey().toString();
        String action = event.getAction().name();
        
        for (Restriction restriction : restrictionsManager.getRestrictions()) {
            if (restriction.getType() == Restriction.RestrictionType.ITEM &&
                restriction.getItems().contains(itemType)) {
                
                if (restriction.getActions().contains("USE") && 
                    (action.contains("RIGHT_CLICK") || action.contains("LEFT_CLICK"))) {
                    if (checkRestriction(player, restriction, "restrictions-blocked-item")) {
                        event.setCancelled(true);
                        return;
                    }
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
}
