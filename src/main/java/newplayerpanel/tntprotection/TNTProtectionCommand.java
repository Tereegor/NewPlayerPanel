package newplayerpanel.tntprotection;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TNTProtectionCommand implements CommandExecutor, TabCompleter {
    
    private final TNTProtectionManager protectionManager;
    
    public TNTProtectionCommand(TNTProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.tntprotection.manage")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды!");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§6=== TNT Protection ===");
            sender.sendMessage("§7/tntprotection time [игрок]");
            sender.sendMessage("§7/tntprotection settings");
            sender.sendMessage("§7/tntprotection reload");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("time") || subCommand.equals("check")) {
            handleTimeCommand(sender, args);
        } else if (subCommand.equals("settings") || subCommand.equals("info")) {
            handleSettingsCommand(sender);
        } else if (subCommand.equals("reload")) {
            protectionManager.loadConfig();
            sender.sendMessage("§aКонфигурация перезагружена!");
        } else {
            sender.sendMessage("§cНеизвестная подкоманда!");
        }
        
        return true;
    }
    
    private void handleTimeCommand(CommandSender sender, String[] args) {
        Player targetPlayer = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
        if (targetPlayer == null) {
            sender.sendMessage("§cИгрок не найден!");
            return;
        }
        
        UUID playerUUID = targetPlayer.getUniqueId();
        long playTimeHours = protectionManager.getPlayerPlayTimeHours(playerUUID);
        
        sender.sendMessage("§6=== Время игрока: " + targetPlayer.getName() + " ===");
        sender.sendMessage("§7Часов на сервере: §e" + playTimeHours);
        sender.sendMessage("§7TNT: " + formatRestriction(playTimeHours, "place-tnt"));
        sender.sendMessage("§7Огниво: " + formatRestriction(playTimeHours, "use-flint-and-steel"));
        sender.sendMessage("§7Огненный шар: " + formatRestriction(playTimeHours, "use-fireball"));
        sender.sendMessage("§7Вагонетка с TNT: " + formatRestriction(playTimeHours, "use-tnt-minecart"));
    }
    
    private String formatRestriction(long playTimeHours, String restrictionKey) {
        int required = protectionManager.getRequiredHours(restrictionKey);
        if (required == 0) return "§aдоступно";
        boolean canUse = playTimeHours >= required;
        return (canUse ? "§a" : "§c") + (canUse ? "доступно" : "недоступно") + " §7(нужно: " + required + " ч)";
    }
    
    private void handleSettingsCommand(CommandSender sender) {
        sender.sendMessage("§6=== Настройки TNT Protection ===");
        sender.sendMessage("§7Минимальное время: §e" + protectionManager.getMinHours() + " ч");
        sender.sendMessage("§7TNT: §e" + formatHours("place-tnt"));
        sender.sendMessage("§7Огниво: §e" + formatHours("use-flint-and-steel"));
        sender.sendMessage("§7Огненный шар: §e" + formatHours("use-fireball"));
        sender.sendMessage("§7Вагонетка с TNT: §e" + formatHours("use-tnt-minecart"));
    }
    
    private String formatHours(String restrictionKey) {
        int hours = protectionManager.getRequiredHours(restrictionKey);
        return hours > 0 ? hours + " ч" : "отключено";
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String arg = args[0].toLowerCase();
            if ("time".startsWith(arg) || "check".startsWith(arg)) completions.add("time");
            if ("settings".startsWith(arg) || "info".startsWith(arg)) completions.add("settings");
            if ("reload".startsWith(arg)) completions.add("reload");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("check"))) {
            String arg = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(arg)) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}

