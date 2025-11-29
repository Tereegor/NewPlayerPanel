package newplayerpanel.villagertracker;

import net.md_5.bungee.api.chat.TextComponent;
import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryCommand implements CommandExecutor, TabCompleter {
    
    private final VillagerDataManager dataManager;
    private final MessageManager messageManager;
    private final SimpleDateFormat dateFormat;
    
    public HistoryCommand(VillagerDataManager dataManager, MessageManager messageManager) {
        this.dataManager = dataManager;
        this.messageManager = messageManager;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.history")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        
        if (args.length >= 1 && args[0].equalsIgnoreCase("purge")) {
            return handleClearCommand(sender, args);
        }
        
        List<VillagerDeathRecord> records;
        String searchDescription;
        
        if (args.length == 0) {
            records = dataManager.getRecords();
            searchDescription = messageManager.get("tracker-header-all");
        } else if (args.length == 1) {
            String firstArg = args[0];
            
            if (firstArg.equalsIgnoreCase("coords")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(messageManager.get("player-only"));
                    return true;
                }
                Player player = (Player) sender;
                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();
                String world = player.getWorld().getName();
                
                records = dataManager.getRecordsByCoordinates(x, y, z, world);
                String coords = String.format("%.2f, %.2f, %.2f", x, y, z);
                searchDescription = messageManager.get("tracker-header-coords", "coords", coords) +
                    messageManager.get("tracker-world-suffix", "world", world);
            } else {
                try {
                    Double.parseDouble(firstArg);
                    sender.sendMessage(messageManager.get("tracker-coords-usage"));
                    sender.sendMessage(messageManager.get("tracker-usage"));
                    return true;
                } catch (NumberFormatException e) {
                    records = dataManager.getRecordsByPlayer(firstArg);
                    searchDescription = messageManager.get("tracker-header-player", "player", firstArg);
                }
            }
        } else if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                
                String world = null;
                if (sender instanceof Player) {
                    world = ((Player) sender).getWorld().getName();
                }
                
                records = dataManager.getRecordsByCoordinates(x, y, z, world);
                String coords = String.format("%.2f, %.2f, %.2f", x, y, z);
                searchDescription = messageManager.get("tracker-header-coords", "coords", coords);
                if (world != null) {
                    searchDescription += messageManager.get("tracker-world-suffix", "world", world);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(messageManager.get("tracker-coords-usage"));
                sender.sendMessage(messageManager.get("tracker-usage"));
                return true;
            }
        } else if (args.length == 2) {
            String firstArg = args[0];
            String secondArg = args[1];
            
            if (firstArg.equalsIgnoreCase("coords") || secondArg.equalsIgnoreCase("coords")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(messageManager.get("player-only"));
                    return true;
                }
                Player player = (Player) sender;
                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();
                String world = player.getWorld().getName();
                String playerName = firstArg.equalsIgnoreCase("coords") ? secondArg : firstArg;
                
                records = dataManager.getRecordsByPlayerAndCoordinates(playerName, x, y, z, world);
                String coords = String.format("%.2f, %.2f, %.2f", x, y, z);
                searchDescription = messageManager.get("tracker-header-combined", 
                    "player", playerName, "coords", coords) +
                    messageManager.get("tracker-world-suffix", "world", world);
            } else {
                sender.sendMessage(messageManager.get("tracker-usage-full"));
                return true;
            }
        } else if (args.length == 4) {
            String playerName = args[0];
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                
                String world = null;
                if (sender instanceof Player) {
                    world = ((Player) sender).getWorld().getName();
                }
                
                records = dataManager.getRecordsByPlayerAndCoordinates(playerName, x, y, z, world);
                String coords = String.format("%.2f, %.2f, %.2f", x, y, z);
                searchDescription = messageManager.get("tracker-header-combined", 
                    "player", playerName, "coords", coords);
                if (world != null) {
                    searchDescription += messageManager.get("tracker-world-suffix", "world", world);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(messageManager.get("tracker-coords-usage"));
                sender.sendMessage(messageManager.get("tracker-usage"));
                return true;
            }
        } else {
            sender.sendMessage(messageManager.get("tracker-usage-full"));
            return true;
        }
        
        if (records.isEmpty()) {
            sender.sendMessage(messageManager.get("tracker-no-records"));
            return true;
        }
        
        sender.sendMessage(searchDescription);
        sender.sendMessage(messageManager.get("tracker-total-records", "count", String.valueOf(records.size())));
        sender.sendMessage("");
        
        int startIndex = Math.max(0, records.size() - 10);
        for (int i = startIndex; i < records.size(); i++) {
            VillagerDeathRecord record = records.get(i);
            int number = i + 1;
            
            sender.sendMessage(messageManager.get("tracker-entry-header", 
                "number", String.valueOf(number), 
                "player", record.getPlayerName()));
            
            sender.sendMessage(messageManager.get("tracker-entry-type", "type", record.getVillagerType()));
            sender.sendMessage(messageManager.get("tracker-entry-world", "world", record.getWorld()));
            
            String coordsLabel = messageManager.get("tracker-entry-coords");
            TextComponent clickableCoords = messageManager.createClickableCoords(
                record.getWorld(), record.getX(), record.getY(), record.getZ());
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.spigot().sendMessage(
                    new TextComponent(TextComponent.fromLegacyText(coordsLabel)),
                    clickableCoords
                );
            } else {
                sender.sendMessage(coordsLabel + String.format("%.0f, %.0f, %.0f", 
                    record.getX(), record.getY(), record.getZ()));
            }
            
            sender.sendMessage(messageManager.get("tracker-entry-date", 
                "date", dateFormat.format(new Date(record.getTimestamp()))));
            
            if (!record.getEnchantments().isEmpty()) {
                StringBuilder enchants = new StringBuilder();
                record.getEnchantments().forEach((name, level) -> 
                    enchants.append(name).append(" ").append(level).append(", "));
                String enchantsStr = enchants.toString();
                if (enchantsStr.endsWith(", ")) {
                    enchantsStr = enchantsStr.substring(0, enchantsStr.length() - 2);
                }
                sender.sendMessage(messageManager.get("tracker-entry-enchantments", "enchantments", enchantsStr));
            }
            
            sender.sendMessage("");
        }
        
        if (records.size() > 10) {
            sender.sendMessage(messageManager.get("tracker-shown-last", 
                "shown", "10", 
                "total", String.valueOf(records.size())));
        }
        
        return true;
    }
    
    private boolean handleClearCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newplayerpanel.history.purge")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(messageManager.get("tracker-purge-usage"));
            return true;
        }
        
        String timeString = args[1];
        long seconds = TimeUtil.parseTimeString(timeString);
        
        if (seconds <= 0) {
            sender.sendMessage(messageManager.get("tracker-purge-invalid-time"));
            return true;
        }
        
        long olderThanTimestamp = System.currentTimeMillis() - (seconds * 1000L);
        int deletedCount = dataManager.clearOldRecords(olderThanTimestamp);
        
        if (deletedCount >= 0) {
            sender.sendMessage(messageManager.get("tracker-purge-success", 
                "count", String.valueOf(deletedCount),
                "time", timeString));
        } else {
            sender.sendMessage(messageManager.get("database-error"));
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            if ("purge".startsWith(input)) {
                completions.add("purge");
            }
            
            if ("coords".startsWith(input)) {
                completions.add("coords");
            }
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("purge")) {
                completions.add("1h");
                completions.add("1d");
                completions.add("7d");
                completions.add("30d");
            } else if (args[0].equalsIgnoreCase("coords")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
