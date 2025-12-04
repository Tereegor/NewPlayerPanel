package newplayerpanel.villagertracker;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.ActionBarUtil;
import newplayerpanel.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HistoryCommand implements CommandExecutor, TabCompleter {
    
    private final VillagerDataManager dataManager;
    private final MessageManager messageManager;
    private final JavaPlugin plugin;
    private final SimpleDateFormat dateFormat;
    private final Map<UUID, List<VillagerDeathRecord>> lastShownRecords = new HashMap<>();
    
    public HistoryCommand(VillagerDataManager dataManager, MessageManager messageManager, JavaPlugin plugin) {
        this.dataManager = dataManager;
        this.messageManager = messageManager;
        this.plugin = plugin;
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
                double x = Math.round(player.getLocation().getX());
                double y = Math.round(player.getLocation().getY());
                double z = Math.round(player.getLocation().getZ());
                String world = player.getWorld().getName();
                
                records = dataManager.getRecordsByCoordinates(x, y, z, world);
                String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
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
                String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
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
                double x = Math.round(player.getLocation().getX());
                double y = Math.round(player.getLocation().getY());
                double z = Math.round(player.getLocation().getZ());
                String world = player.getWorld().getName();
                String playerName = firstArg.equalsIgnoreCase("coords") ? secondArg : firstArg;
                
                records = dataManager.getRecordsByPlayerAndCoordinates(playerName, x, y, z, world);
                String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
                searchDescription = messageManager.get("tracker-header-combined", 
                    "player", playerName, "coords", coords) +
                    messageManager.get("tracker-world-suffix", "world", world);
            } else {
                String usageFull = messageManager.get("tracker-usage-full");
                String[] lines = usageFull.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        sender.sendMessage(line);
                    }
                }
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
                String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
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
            String usageFull = messageManager.get("tracker-usage-full");
            String[] lines = usageFull.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sender.sendMessage(line);
                }
            }
            return true;
        }
        
        if (records.isEmpty()) {
            sender.sendMessage(messageManager.get("tracker-no-records"));
            return true;
        }
        
        sender.sendMessage(searchDescription);
        sender.sendMessage(messageManager.get("tracker-total-records", "count", String.valueOf(records.size())));
        sender.sendMessage("");
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            lastShownRecords.put(player.getUniqueId(), new ArrayList<>(records));
        }
        
        int startIndex = Math.max(0, records.size() - 10);
        for (int i = startIndex; i < records.size(); i++) {
            VillagerDeathRecord record = records.get(i);
            int number = i - startIndex + 1;
            
            sender.sendMessage(messageManager.get("tracker-entry-header", 
                "number", String.valueOf(number), 
                "player", record.getPlayerName()));
            
            sender.sendMessage(messageManager.get("tracker-entry-type", "type", record.getVillagerType()));
            sender.sendMessage(messageManager.get("tracker-entry-level", "level", String.valueOf(record.getVillagerLevel())));
            sender.sendMessage(messageManager.get("tracker-entry-world", "world", record.getWorld()));
            
            String coordsLabel = messageManager.get("tracker-entry-coords");
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                try {
                    net.kyori.adventure.text.Component labelComponent = messageManager.getComponent("tracker-entry-coords");
                    net.kyori.adventure.text.Component villagerTpComponent = messageManager.createClickableVillagerTpComponent(
                        record.getWorld(), record.getX(), record.getY(), record.getZ());
                    net.kyori.adventure.text.Component fullComponent = labelComponent.append(villagerTpComponent);
                    
                    java.lang.reflect.Method sendMethod = Player.class.getMethod("sendMessage", net.kyori.adventure.text.Component.class);
                    sendMethod.invoke(player, fullComponent);
                } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException e) {
                    try {
                        net.md_5.bungee.api.chat.TextComponent labelComponent = new net.md_5.bungee.api.chat.TextComponent(
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(coordsLabel));
                        net.md_5.bungee.api.chat.TextComponent villagerTpComponent = messageManager.createClickableVillagerTpComponentSpigot(
                            record.getWorld(), record.getX(), record.getY(), record.getZ());
                        
                        net.md_5.bungee.api.chat.BaseComponent[] fullMessage = new net.md_5.bungee.api.chat.BaseComponent[]{
                            labelComponent,
                            villagerTpComponent
                        };
                        
                        player.spigot().sendMessage(fullMessage);
                    } catch (Exception ex) {
                        sender.sendMessage(coordsLabel + String.format("%.0f, %.0f, %.0f", 
                            record.getX(), record.getY(), record.getZ()));
                    }
                }
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
            
            if (!record.getTrades().isEmpty()) {
                sender.sendMessage(messageManager.get("tracker-entry-trades-header"));
                for (int tradeIndex = 0; tradeIndex < record.getTrades().size(); tradeIndex++) {
                    Map<String, Object> trade = record.getTrades().get(tradeIndex);
                    StringBuilder tradeInfo = new StringBuilder();
                    tradeInfo.append("  &7[&e").append(tradeIndex + 1).append("&7] ");
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> ingredients = (List<Map<String, Object>>) trade.get("ingredients");
                    if (ingredients != null && !ingredients.isEmpty()) {
                        for (Map<String, Object> ing : ingredients) {
                            String type = (String) ing.get("type");
                            int amount = ((Double) ing.get("amount")).intValue();
                            String itemName = type.substring(type.lastIndexOf(':') + 1);
                            tradeInfo.append("&7").append(amount).append("x &f").append(itemName).append(" &7→ ");
                        }
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) trade.get("result");
                    if (result != null) {
                        String type = (String) result.get("type");
                        int amount = ((Double) result.get("amount")).intValue();
                        String itemName = type.substring(type.lastIndexOf(':') + 1);
                        tradeInfo.append("&a").append(amount).append("x ").append(itemName);
                    }
                    
                    if (sender instanceof Player) {
                        ActionBarUtil.sendMessage((Player) sender, tradeInfo.toString());
                    } else {
                        sender.sendMessage(tradeInfo.toString());
                    }
                }
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
    
    private ItemStack createItemStackFromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        
        try {
            String typeStr = (String) data.get("type");
            if (typeStr == null) {
                typeStr = (String) data.get("material");
            }
            if (typeStr == null) {
                return null;
            }
            
            Material material;
            if (typeStr.contains(":")) {
                NamespacedKey key = NamespacedKey.fromString(typeStr);
                if (key != null) {
                    material = Material.matchMaterial(key.getKey());
                } else {
                    material = Material.matchMaterial(typeStr);
                }
            } else {
                material = Material.matchMaterial(typeStr);
            }
            
            if (material == null) {
                return null;
            }
            
            Object amountObj = data.get("amount");
            int amount = 1;
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).intValue();
            } else if (amountObj instanceof String) {
                try {
                    amount = Integer.parseInt((String) amountObj);
                } catch (NumberFormatException e) {
                    amount = 1;
                }
            }
            
            ItemStack item = new ItemStack(material, amount);
            
            if (item.hasItemMeta() && data.containsKey("displayName")) {
                ItemMeta meta = item.getItemMeta();
                String displayName = (String) data.get("displayName");
                if (displayName != null && !displayName.isEmpty()) {
                    meta.setDisplayName(displayName);
                }
                
                if (data.containsKey("lore")) {
                    @SuppressWarnings("unchecked")
                    List<String> lore = (List<String>) data.get("lore");
                    if (lore != null && !lore.isEmpty()) {
                        meta.setLore(lore);
                    }
                }
                
                if (data.containsKey("enchants")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> enchants = (Map<String, Integer>) data.get("enchants");
                    if (enchants != null && !enchants.isEmpty()) {
                        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                                try {
                                    NamespacedKey enchantKey = NamespacedKey.fromString(entry.getKey());
                                    if (enchantKey != null) {
                                        org.bukkit.enchantments.Enchantment enchant = null;
                                        try {
                                            enchant = org.bukkit.Registry.ENCHANTMENT.get(enchantKey);
                                        } catch (Exception e1) {
                                            try {
                                                enchant = org.bukkit.enchantments.Enchantment.getByKey(enchantKey);
                                            } catch (Exception e2) {
                                            }
                                        }
                                        if (enchant != null) {
                                            meta.addEnchant(enchant, entry.getValue(), true);
                                        }
                                    }
                                } catch (Exception e) {
                                }
                        }
                    }
                }
                
                item.setItemMeta(meta);
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create ItemStack from data: " + e.getMessage());
            return null;
        }
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
                String input = args[1].toLowerCase();
                String[] timeOptions = {"1h", "1d", "7d", "30d"};
                for (String option : timeOptions) {
                    if (option.toLowerCase().startsWith(input)) {
                        completions.add(option);
                    }
                }
            } else if (args[0].equalsIgnoreCase("coords")) {
                String input = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
