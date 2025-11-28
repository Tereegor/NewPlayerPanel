package newplayerpanel.restrictions;

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
    
    public RestrictCommand(RestrictionsManager restrictionsManager) {
        this.restrictionsManager = restrictionsManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.restrictions.restrict")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды!");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: /restrict <игрок> <ограничение> <время в секундах>");
            sender.sendMessage("§70 = снять, -1 = перманентно, >0 = время в секундах");
            return true;
        }
        
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage("§cИгрок не найден!");
            return true;
        }
        
        Restriction restriction = restrictionsManager.getRestrictionByName(args[1]);
        if (restriction == null) {
            sender.sendMessage("§cОграничение не найдено!");
            return true;
        }
        
        long durationSeconds;
        try {
            durationSeconds = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверный формат времени!");
            return true;
        }
        
        UUID playerUUID = targetPlayer.getUniqueId();
        String restrictionName = restriction.getName();
        
        if (durationSeconds == 0) {
            restrictionsManager.removePlayerRestriction(playerUUID, restrictionName);
            sender.sendMessage("§aОграничение снято с " + targetPlayer.getName());
            ActionBarUtil.sendActionBar(targetPlayer, "§a[NewPlayerPanel] С вас снято ограничение: " + restrictionName);
        } else {
            restrictionsManager.addPlayerRestriction(playerUUID, restrictionName, durationSeconds);
            String timeStr = TimeUtil.formatTime(durationSeconds);
            sender.sendMessage("§aОграничение применено: " + timeStr);
            ActionBarUtil.sendActionBar(targetPlayer, "§c[NewPlayerPanel] Ограничение: " + restrictionName + " (" + timeStr + ")");
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
            completions.add("0");
            completions.add("-1");
        }
        
        return completions;
    }
}

