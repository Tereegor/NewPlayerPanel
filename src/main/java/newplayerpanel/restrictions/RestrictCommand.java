package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.ActionBarUtil;
import newplayerpanel.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RestrictCommand implements CommandExecutor, TabCompleter {
    
    private final RestrictionsManager restrictionsManager;
    private final MessageManager messageManager;
    
    public RestrictCommand(RestrictionsManager restrictionsManager, MessageManager messageManager) {
        this.restrictionsManager = restrictionsManager;
        this.messageManager = messageManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.restrictions.restrict")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(messageManager.get("restrictions-usage"));
            sender.sendMessage(messageManager.get("restrictions-usage-time"));
            return true;
        }
        
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(messageManager.get("player-not-found"));
            return true;
        }
        
        Restriction restriction = restrictionsManager.getRestrictionByName(args[1]);
        if (restriction == null) {
            sender.sendMessage(messageManager.get("restrictions-not-found"));
            return true;
        }
        
        long durationSeconds = TimeUtil.parseTimeInput(args[2]);
        if (durationSeconds == Long.MIN_VALUE) {
            sender.sendMessage(messageManager.get("invalid-number"));
            return true;
        }
        
        UUID playerUUID = targetPlayer.getUniqueId();
        String restrictionName = restriction.getName();
        
        if (durationSeconds == 0) {
            restrictionsManager.removePlayerRestriction(playerUUID, restrictionName);
            sender.sendMessage(messageManager.get("restrictions-removed", "player", targetPlayer.getName()));
            ActionBarUtil.sendActionBar(targetPlayer, 
                messageManager.get("restrictions-notify-removed", "restriction", restrictionName));
        } else {
            restrictionsManager.addPlayerRestriction(playerUUID, restrictionName, durationSeconds);
            String timeStr = TimeUtil.formatTimeLocalized(durationSeconds, messageManager);
            sender.sendMessage(messageManager.get("restrictions-applied", "time", timeStr));
            ActionBarUtil.sendActionBar(targetPlayer, 
                messageManager.get("restrictions-notify-applied", "restriction", restrictionName, "time", timeStr));
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            for (Restriction restriction : restrictionsManager.getRestrictions()) {
                if (restriction.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(restriction.getName());
                }
            }
        } else if (args.length == 3) {
            String input = args[2].toLowerCase();
            String[] timeOptions = {"0", "-1", "1h", "1d", "7d"};
            for (String option : timeOptions) {
                if (option.toLowerCase().startsWith(input)) {
                    completions.add(option);
                }
            }
        }
        
        return completions;
    }
}
