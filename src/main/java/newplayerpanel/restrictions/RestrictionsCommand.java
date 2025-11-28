package newplayerpanel.restrictions;

import newplayerpanel.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RestrictionsCommand implements CommandExecutor {
    
    private final RestrictionsManager restrictionsManager;
    
    public RestrictionsCommand(RestrictionsManager restrictionsManager) {
        this.restrictionsManager = restrictionsManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.restrictions.view")) {
            sender.sendMessage("§cУ вас нет прав!");
            return true;
        }
        
        Player targetPlayer = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player ? (Player) sender : null);
        if (targetPlayer == null) {
            sender.sendMessage("§cИгрок не найден!");
            return true;
        }
        
        List<PlayerRestriction> active = restrictionsManager.getPlayerRestrictions(targetPlayer.getUniqueId());
        if (active.isEmpty()) {
            sender.sendMessage("§7Нет активных ограничений.");
            return true;
        }
        
        sender.sendMessage("§6=== Ограничения: " + targetPlayer.getName() + " ===");
        for (PlayerRestriction pr : active) {
            sender.sendMessage("§7- §e" + pr.getRestrictionName() + " §7(§f" + TimeUtil.formatTime(pr.getRemainingSeconds()) + "§7)");
        }
        
        return true;
    }
}

