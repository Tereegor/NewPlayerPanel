package newplayerpanel.villagertracker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VillagerHistoryCommand implements CommandExecutor {
    
    private final VillagerDataManager dataManager;
    private final SimpleDateFormat dateFormat;
    
    public VillagerHistoryCommand(VillagerDataManager dataManager) {
        this.dataManager = dataManager;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newplayerpanel.villagertracker.history")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды!");
            return true;
        }
        
        List<VillagerDeathRecord> records;
        String searchDescription;
        
        if (args.length == 0) {
            records = dataManager.getRecords();
            searchDescription = "§6=== История всех убийств жителей ===";
        } else if (args.length == 1) {
            String firstArg = args[0];
            
            if (firstArg.equalsIgnoreCase("coords")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cОшибка: Эта команда может быть использована только игроком!");
                    return true;
                }
                Player player = (Player) sender;
                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();
                String world = player.getWorld().getName();
                
                records = dataManager.getRecordsByCoordinates(x, y, z, world);
                searchDescription = "§6=== Убийства жителей в радиусе ±2 блок от ваших координат " + 
                    String.format("%.2f, %.2f, %.2f", x, y, z) + " ===";
                searchDescription += " (мир: " + world + ")";
            } else {
                try {
                    Double.parseDouble(firstArg);
                    sender.sendMessage("§cОшибка: Для поиска по координатам укажите X Y Z или используйте 'coords'");
                    sender.sendMessage("§7Использование: /villagerhistory <игрок> или /villagerhistory <x> <y> <z> или /villagerhistory coords");
                    return true;
                } catch (NumberFormatException e) {
                    records = dataManager.getRecordsByPlayer(firstArg);
                    searchDescription = "§6=== История убийств жителей игрока: " + firstArg + " ===";
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
                searchDescription = "§6=== Убийства жителей в радиусе ±2 блок от координат " + 
                    String.format("%.2f, %.2f, %.2f", x, y, z) + " ===";
                if (world != null) {
                    searchDescription += " (мир: " + world + ")";
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cОшибка: Неверный формат координат!");
                sender.sendMessage("§7Использование: /villagerhistory <x> <y> <z>");
                return true;
            }
        } else if (args.length == 2) {
            String firstArg = args[0];
            String secondArg = args[1];
            
            if (firstArg.equalsIgnoreCase("coords")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cОшибка: Эта команда может быть использована только игроком!");
                    return true;
                }
                Player player = (Player) sender;
                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();
                String world = player.getWorld().getName();
                String playerName = secondArg;
                
                records = dataManager.getRecordsByPlayerAndCoordinates(playerName, x, y, z, world);
                searchDescription = "§6=== Убийства жителей игроком " + playerName + 
                    " в радиусе ±2 блок от ваших координат " + 
                    String.format("%.2f, %.2f, %.2f", x, y, z) + " ===";
                searchDescription += " (мир: " + world + ")";
            } else if (secondArg.equalsIgnoreCase("coords")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cОшибка: Эта команда может быть использована только игроком!");
                    return true;
                }
                Player player = (Player) sender;
                double x = player.getLocation().getX();
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ();
                String world = player.getWorld().getName();
                String playerName = firstArg;
                
                records = dataManager.getRecordsByPlayerAndCoordinates(playerName, x, y, z, world);
                searchDescription = "§6=== Убийства жителей игроком " + playerName + 
                    " в радиусе ±2 блок от ваших координат " + 
                    String.format("%.2f, %.2f, %.2f", x, y, z) + " ===";
                searchDescription += " (мир: " + world + ")";
            } else {
                sender.sendMessage("§cОшибка: Неверное использование команды!");
                sender.sendMessage("§7Использование: /villagerhistory coords [игрок]");
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
                searchDescription = "§6=== Убийства жителей игроком " + playerName + 
                    " в радиусе ±2 блок от координат " + 
                    String.format("%.2f, %.2f, %.2f", x, y, z) + " ===";
                if (world != null) {
                    searchDescription += " (мир: " + world + ")";
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cОшибка: Неверный формат координат!");
                sender.sendMessage("§7Использование: /villagerhistory <игрок> <x> <y> <z>");
                return true;
            }
        } else {
            sender.sendMessage("§cОшибка: Неверное количество аргументов!");
            sender.sendMessage("§7Использование:");
            sender.sendMessage("§7  /villagerhistory - все записи");
            sender.sendMessage("§7  /villagerhistory <игрок> - записи игрока");
            sender.sendMessage("§7  /villagerhistory <x> <y> <z> - записи по координатам");
            sender.sendMessage("§7  /villagerhistory coords - записи по вашим координатам");
            sender.sendMessage("§7  /villagerhistory coords <игрок> - записи игрока по вашим координатам");
            sender.sendMessage("§7  /villagerhistory <игрок> <x> <y> <z> - комбинированный поиск");
            return true;
        }
        
        if (records.isEmpty()) {
            sender.sendMessage("§7Записей не найдено.");
            return true;
        }
        
        sender.sendMessage(searchDescription);
        sender.sendMessage("§7Всего записей: §e" + records.size());
        sender.sendMessage("");
        
        int startIndex = Math.max(0, records.size() - 10);
        for (int i = startIndex; i < records.size(); i++) {
            VillagerDeathRecord record = records.get(i);
            int number = i + 1;
            
            sender.sendMessage("§7[" + number + "] §e" + record.getPlayerName());
            sender.sendMessage("  §7Тип: §f" + record.getVillagerType());
            sender.sendMessage("  §7Мир: §f" + record.getWorld());
            sender.sendMessage("  §7Координаты: §f" + 
                String.format("%.2f, %.2f, %.2f", record.getX(), record.getY(), record.getZ()));
            sender.sendMessage("  §7Дата: §f" + dateFormat.format(new Date(record.getTimestamp())));
            
            if (!record.getEnchantments().isEmpty()) {
                StringBuilder enchants = new StringBuilder("  §7Зачарования: §f");
                record.getEnchantments().forEach((name, level) -> 
                    enchants.append(name).append(" ").append(level).append(", "));
                String enchantsStr = enchants.toString();
                if (enchantsStr.endsWith(", ")) {
                    enchantsStr = enchantsStr.substring(0, enchantsStr.length() - 2);
                }
                sender.sendMessage(enchantsStr);
            }
            
            sender.sendMessage("");
        }
        
        if (records.size() > 10) {
            sender.sendMessage("§7Показаны последние 10 записей из " + records.size());
        }
        
        return true;
    }
}

