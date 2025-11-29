package newplayerpanel.restrictions;

import newplayerpanel.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnrestrictCommand implements CommandExecutor, TabCompleter {
    
    private final RestrictionsManager restrictionsManager;
    private final MessageManager messageManager;
    
    public UnrestrictCommand(RestrictionsManager restrictionsManager, MessageManager messageManager) {
        this.restrictionsManager = restrictionsManager;
        this.messageManager = messageManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.restrictions.restrict")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(messageManager.get("restrictions-usage"));
            return true;
        }
        
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(messageManager.get("player-not-found"));
            return true;
        }
        
        UUID playerUUID = targetPlayer.getUniqueId();
        
        if (args[1].equalsIgnoreCase("all")) {
            List<PlayerRestriction> active = restrictionsManager.getPlayerRestrictions(playerUUID);
            if (active.isEmpty()) {
                sender.sendMessage(messageManager.get("restrictions-no-active"));
                return true;
            }
            
            active.forEach(pr -> restrictionsManager.removePlayerRestriction(playerUUID, pr.getRestrictionName()));
            sender.sendMessage(messageManager.get("restrictions-removed-all"));
            targetPlayer.sendMessage(messageManager.get("restrictions-notify-removed-all"));
        } else {
            restrictionsManager.removePlayerRestriction(playerUUID, args[1]);
            sender.sendMessage(messageManager.get("restrictions-removed", "player", targetPlayer.getName()));
            targetPlayer.sendMessage(messageManager.get("restrictions-notify-removed", "restriction", args[1]));
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
            Player targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer != null) {
                completions.add("all");
                for (PlayerRestriction pr : restrictionsManager.getPlayerRestrictions(targetPlayer.getUniqueId())) {
                    if (pr.getRestrictionName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(pr.getRestrictionName());
                    }
                }
            }
        }
        
        return completions;
    }
}
