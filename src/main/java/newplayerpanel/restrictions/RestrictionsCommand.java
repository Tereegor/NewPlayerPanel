package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RestrictionsCommand implements CommandExecutor {
    
    private final RestrictionsManager restrictionsManager;
    private final MessageManager messageManager;
    
    public RestrictionsCommand(RestrictionsManager restrictionsManager, MessageManager messageManager) {
        this.restrictionsManager = restrictionsManager;
        this.messageManager = messageManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.restrictions.view")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        
        Player targetPlayer = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player ? (Player) sender : null);
        if (targetPlayer == null) {
            sender.sendMessage(messageManager.get("player-not-found"));
            return true;
        }
        
        List<PlayerRestriction> active = restrictionsManager.getPlayerRestrictions(targetPlayer.getUniqueId());
        if (active.isEmpty()) {
            sender.sendMessage(messageManager.get("restrictions-no-active"));
            return true;
        }
        
        sender.sendMessage(messageManager.get("restrictions-header", "player", targetPlayer.getName()));
        
        for (PlayerRestriction pr : active) {
            String timeFormatted = TimeUtil.formatTimeLocalized(pr.getRemainingSeconds(), messageManager);
            sender.sendMessage(messageManager.get("restrictions-entry", 
                "restriction", pr.getRestrictionName(), 
                "time", timeFormatted));
        }
        
        return true;
    }
}
