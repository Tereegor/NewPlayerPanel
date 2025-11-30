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
        Player targetPlayer;
        
        if (args.length > 0) {
            if (!sender.hasPermission("newplayerpanel.restrictions.view.others")) {
                if (sender instanceof Player && ((Player) sender).getName().equalsIgnoreCase(args[0])) {
                    targetPlayer = (Player) sender;
                } else {
                    sender.sendMessage(messageManager.get("no-permission"));
                    return true;
                }
            } else {
                targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage(messageManager.get("player-not-found"));
                    return true;
                }
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messageManager.get("player-not-found"));
                return true;
            }
            if (!sender.hasPermission("newplayerpanel.restrictions.view")) {
                sender.sendMessage(messageManager.get("no-permission"));
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        List<PlayerRestriction> activePersonal = restrictionsManager.getPlayerRestrictions(targetPlayer.getUniqueId());
        List<Restriction> activeDefaults = restrictionsManager.getActiveDefaultRestrictions(targetPlayer.getUniqueId());
        
        if (activePersonal.isEmpty() && activeDefaults.isEmpty()) {
            sender.sendMessage(messageManager.get("restrictions-no-active"));
            return true;
        }
        
        sender.sendMessage(messageManager.get("restrictions-header", "player", targetPlayer.getName()));
        
        for (PlayerRestriction pr : activePersonal) {
            String timeFormatted = TimeUtil.formatTimeLocalized(pr.getRemainingSeconds(), messageManager);
            sender.sendMessage(messageManager.get("restrictions-entry", 
                "restriction", pr.getRestrictionName(), 
                "time", timeFormatted));
        }
        
        if (!activeDefaults.isEmpty()) {
            sender.sendMessage(messageManager.get("restrictions-defaults-header"));
            
            for (Restriction restriction : activeDefaults) {
                long remainingTime = restrictionsManager.getDefaultRestrictionRemainingTime(targetPlayer.getUniqueId(), restriction.getName());
                String timeFormatted = TimeUtil.formatTimeLocalized(remainingTime, messageManager);
                sender.sendMessage(messageManager.get("restrictions-entry-default", 
                    "restriction", restriction.getName(), 
                    "time", timeFormatted));
            }
        }
        
        return true;
    }
}
