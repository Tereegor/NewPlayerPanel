package newplayerpanel.spawnprotect;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.ActionBarUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SpawnProtectListener implements Listener {
    
    private final SpawnProtectManager manager;
    private final MessageManager messageManager;
    
    public SpawnProtectListener(SpawnProtectManager manager, MessageManager messageManager) {
        this.manager = manager;
        this.messageManager = messageManager;
    }
    
    private boolean canBypass(Player player) {
        if (player.hasPermission("newplayerpanel.spawnprotect.bypass")) {
            return true;
        }
        return manager.hasPlaytimeBypass(player);
    }
    
    private void sendDenialMessage(Player player, String messageKey) {
        String message = messageManager.get(messageKey);
        if (message != null && !message.isEmpty() && !message.equals(messageKey)) {
            ActionBarUtil.sendActionBar(player, message);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!manager.isEnabled()) return;
        
        Player player = event.getPlayer();
        if (canBypass(player)) return;
        
        Location location = event.getBlock().getLocation();
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        if (!zone.canBreakBlock(event.getBlock().getType())) {
            event.setCancelled(true);
            sendDenialMessage(player, "spawnprotect-block-break");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!manager.isEnabled()) return;
        
        Player player = event.getPlayer();
        if (canBypass(player)) return;
        
        Location location = event.getBlock().getLocation();
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        if (!zone.canPlaceBlock(event.getBlock().getType())) {
            event.setCancelled(true);
            sendDenialMessage(player, "spawnprotect-block-place");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!manager.isEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        if (canBypass(player)) return;
        
        Location location = event.getClickedBlock().getLocation();
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        Material blockType = event.getClickedBlock().getType();
        if (!zone.canInteract(blockType)) {
            event.setCancelled(true);
            sendDenialMessage(player, "spawnprotect-interact");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!manager.isEnabled()) return;
        
        Player player = event.getPlayer();
        if (canBypass(player)) return;
        
        Location location = event.getRightClicked().getLocation();
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        if (!zone.canInteractEntity(event.getRightClicked().getType())) {
            event.setCancelled(true);
            sendDenialMessage(player, "spawnprotect-entity-interact");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!manager.isEnabled()) return;
        
        Location location = event.getBlock().getLocation();
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        if (zone.isFireSpreadEnabled()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!manager.isEnabled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        Location location = victim.getLocation();
        
        Entity damager = event.getDamager();
        Player attacker = null;
        
        if (damager instanceof Player) {
            attacker = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        if (attacker == null || canBypass(attacker)) return;
        
        SpawnZone victimZone = manager.getZoneAt(location);
        SpawnZone attackerZone = manager.getZoneAt(attacker.getLocation());
        
        if ((victimZone != null && victimZone.isPvpEnabled()) || 
            (attackerZone != null && attackerZone.isPvpEnabled())) {
            event.setCancelled(true);
            sendDenialMessage(attacker, "spawnprotect-pvp");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!manager.isEnabled()) return;
        
        Location location = event.getLocation();
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        if (zone.isExplosionsEnabled()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        if (!manager.isEnabled()) return;
        
        Location location = event.getBlock().getLocation();
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        if (zone.isExplosionsEnabled()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!manager.isEnabled()) return;
        
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        
        SpawnZone zone = manager.getZoneAt(location);
        if (zone == null) return;
        
        if (zone.isExplosionsEnabled()) {
            if (entity instanceof TNTPrimed || entity instanceof EnderCrystal || 
                entity.getType() == EntityType.MINECART_TNT) {
                event.setCancelled(true);
            }
        }
    }
}
