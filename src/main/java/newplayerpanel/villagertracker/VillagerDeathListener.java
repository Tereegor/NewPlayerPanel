package newplayerpanel.villagertracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import newplayerpanel.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VillagerDeathListener implements Listener {
    
    private final JavaPlugin plugin;
    private final VillagerDataManager dataManager;
    private final MessageManager messageManager;
    
    public VillagerDeathListener(JavaPlugin plugin, VillagerDataManager dataManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = messageManager;
    }

    private Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        if (item == null || !item.hasItemMeta()) {
            return enchants;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta.hasEnchants()) {
            enchants.putAll(meta.getEnchants());
        }

        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
            enchants.putAll(storageMeta.getStoredEnchants());
        }

        return enchants;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }
        
        Villager villager = (Villager) event.getEntity();
        
        Player killer = villager.getKiller();
        if (killer == null) {
            return;
        }
        
        boolean onlyTraded = plugin.getConfig().getBoolean("villager-tracker.only-traded", true);
        
        if (onlyTraded) {
            boolean hasTraded = false;
            for (MerchantRecipe recipe : villager.getRecipes()) {
                if (recipe.getUses() > 0) {
                    hasTraded = true;
                    break;
                }
            }
            
            if (!hasTraded) {
                return;
            }
        }
        
        String villagerType;
        if (villager.getType() != org.bukkit.entity.EntityType.VILLAGER) {
            villagerType = villager.getType().getKey().getKey();
        } else {
            villagerType = villager.getProfession().getKey().getKey();
        }
        
        String world = villager.getWorld().getName();
        double x = Math.round(villager.getLocation().getX());
        double y = Math.round(villager.getLocation().getY());
        double z = Math.round(villager.getLocation().getZ());
        
        Map<String, Integer> enchantments = new HashMap<>();
        
        List<Map<String, Object>> enchantedItems = new ArrayList<>();
        
        for (ItemStack item : villager.getInventory().getContents()) {
            Map<Enchantment, Integer> itemEnchants = getEnchantments(item);
            
            if (!itemEnchants.isEmpty()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("itemType", item.getType().getKey().toString());
                itemData.put("itemMaterial", item.getType().name());
                itemData.put("amount", item.getAmount());
                
                Map<String, Integer> itemEnchantmentsMap = new HashMap<>();
                itemEnchants.forEach((enchant, level) -> {
                    String enchantKey = enchant.getKey().toString();
                    itemEnchantmentsMap.put(enchantKey, level);
                    enchantments.put(enchantKey, level);
                });
                itemData.put("enchantments", itemEnchantmentsMap);
                enchantedItems.add(itemData);
            }
        }
        
        for (MerchantRecipe recipe : villager.getRecipes()) {
            ItemStack result = recipe.getResult();
            getEnchantments(result).forEach((enchant, level) -> {
                String enchantKey = enchant.getKey().toString();
                if (!enchantments.containsKey(enchantKey)) {
                    enchantments.put(enchantKey, level);
                }
            });
            
            for (ItemStack ingredient : recipe.getIngredients()) {
                getEnchantments(ingredient).forEach((enchant, level) -> {
                    String enchantKey = enchant.getKey().toString();
                    if (!enchantments.containsKey(enchantKey)) {
                        enchantments.put(enchantKey, level);
                    }
                });
            }
        }
        
        if (!enchantments.isEmpty()) {
            plugin.getLogger().info("Collected " + enchantments.size() + " unique enchantments from villager inventory and trades: " + enchantments);
        } else {
            plugin.getLogger().fine("No enchantments found on villager items or trades");
        }
        List<Map<String, Object>> trades = new ArrayList<>();
        int tradeIndex = 0;
        for (MerchantRecipe recipe : villager.getRecipes()) {
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("index", tradeIndex++);
            
            ItemStack result = recipe.getResult();
            if (result != null) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("type", result.getType().getKey().toString());
                resultData.put("material", result.getType().name());
                resultData.put("amount", result.getAmount());
                resultData.put("maxStackSize", result.getMaxStackSize());
                
                if (result.hasItemMeta()) {
                    ItemMeta meta = result.getItemMeta();
                    if (meta.hasDisplayName()) {
                        resultData.put("displayName", meta.getDisplayName());
                    }
                    if (meta.hasLore()) {
                        resultData.put("lore", meta.getLore());
                    }
                    
                    Map<String, Integer> resultEnchants = new HashMap<>();
                    getEnchantments(result).forEach((enchant, level) -> 
                        resultEnchants.put(enchant.getKey().toString(), level));

                    if (!resultEnchants.isEmpty()) {
                        resultData.put("enchants", resultEnchants);
                    }
                }
                tradeData.put("result", resultData);
            }
            
            List<Map<String, Object>> ingredients = new ArrayList<>();
            int ingIndex = 0;
            for (ItemStack ingredient : recipe.getIngredients()) {
                if (ingredient != null) {
                    Map<String, Object> ingData = new HashMap<>();
                    ingData.put("index", ingIndex++);
                    ingData.put("type", ingredient.getType().getKey().toString());
                    ingData.put("material", ingredient.getType().name());
                    ingData.put("amount", ingredient.getAmount());
                    ingData.put("maxStackSize", ingredient.getMaxStackSize());
                    
                    if (ingredient.hasItemMeta()) {
                        ItemMeta meta = ingredient.getItemMeta();
                        if (meta.hasDisplayName()) {
                            ingData.put("displayName", meta.getDisplayName());
                        }
                        if (meta.hasLore()) {
                            ingData.put("lore", meta.getLore());
                        }
                        
                        Map<String, Integer> ingEnchants = new HashMap<>();
                        getEnchantments(ingredient).forEach((enchant, level) -> 
                            ingEnchants.put(enchant.getKey().toString(), level));
                        
                        if (!ingEnchants.isEmpty()) {
                            ingData.put("enchants", ingEnchants);
                        }
                    }
                    ingredients.add(ingData);
                }
            }
            tradeData.put("ingredients", ingredients);
            tradeData.put("ingredientCount", ingredients.size());
            
            tradeData.put("uses", recipe.getUses());
            tradeData.put("maxUses", recipe.getMaxUses());
            tradeData.put("remainingUses", recipe.getMaxUses() - recipe.getUses());
            tradeData.put("hasExperienceReward", recipe.hasExperienceReward());
            tradeData.put("villagerExperience", recipe.getVillagerExperience());
            tradeData.put("priceMultiplier", recipe.getPriceMultiplier());
            try {
                tradeData.put("demand", recipe.getDemand());
            } catch (Exception e) {
            }
            try {
                tradeData.put("specialPrice", recipe.getSpecialPrice());
            } catch (Exception e) {
            }
            
            trades.add(tradeData);
        }
        
        int villagerLevel = villager.getVillagerLevel();
        
        Map<String, Object> fullVillagerData = new HashMap<>();
        fullVillagerData.put("timestamp", System.currentTimeMillis());
        fullVillagerData.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        fullVillagerData.put("killer", new HashMap<String, Object>() {{
            put("name", killer.getName());
            put("uuid", killer.getUniqueId().toString());
            put("displayName", killer.getDisplayName());
        }});
        fullVillagerData.put("villager", new HashMap<String, Object>() {{
            put("profession", villager.getProfession().name());
            put("villagerType", villager.getType().name());
            put("level", villagerLevel);
            put("experience", villager.getVillagerExperience());
            put("type", villagerType);
        }});
        fullVillagerData.put("location", new HashMap<String, Object>() {{
            put("world", world);
            put("x", x);
            put("y", y);
            put("z", z);
            put("pitch", villager.getLocation().getPitch());
            put("yaw", villager.getLocation().getYaw());
        }});
        fullVillagerData.put("enchantments", enchantments);
        fullVillagerData.put("enchantedItems", enchantedItems);
        fullVillagerData.put("enchantedItemsCount", enchantedItems.size());
        fullVillagerData.put("trades", trades);
        fullVillagerData.put("tradeCount", trades.size());
        fullVillagerData.put("inventory", new HashMap<String, Object>() {{
            List<Map<String, Object>> items = new ArrayList<>();
            for (ItemStack item : villager.getInventory().getContents()) {
                if (item != null) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("type", item.getType().getKey().toString());
                    itemData.put("material", item.getType().name());
                    itemData.put("amount", item.getAmount());
                    
                    Map<String, Integer> itemEnchants = new HashMap<>();
                    getEnchantments(item).forEach((enchant, level) -> {
                        String enchantKey = enchant.getKey().toString();
                        itemEnchants.put(enchantKey, level);
                    });
                    
                    if (!itemEnchants.isEmpty()) {
                        itemData.put("enchantments", itemEnchants);
                    }
                    
                    items.add(itemData);
                }
            }
            put("items", items);
            put("itemCount", items.size());
        }});
        
        if (plugin.getConfig().getBoolean("villager-tracker.save-debug-files", false)) {
            saveFullRecordToFile(fullVillagerData);
        }
        
        VillagerDeathRecord record = new VillagerDeathRecord(
            killer.getName(),
            killer.getUniqueId().toString(),
            villagerType,
            world,
            x, y, z,
            enchantments,
            trades,
            villagerLevel
        );
        
        dataManager.addRecord(record);
        
        boolean notifyEnabled = plugin.getConfig().getBoolean("villager-tracker.notify-enabled", true);
        if (notifyEnabled) {
            notifyPlayers(killer.getName(), villagerType, world, x, y, z);
        }
    }
    
    private void saveFullRecordToFile(Map<String, Object> fullData) {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File testFolder = new File(dataFolder, "villager-deaths-test");
            if (!testFolder.exists()) {
                testFolder.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "villager-death-" + timestamp + "-" + 
                ((Map<?, ?>) fullData.get("killer")).get("name") + ".json";
            File testFile = new File(testFolder, fileName);
            
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
            
            try (FileWriter writer = new FileWriter(testFile)) {
                gson.toJson(fullData, writer);
                plugin.getLogger().info("Saved full villager death record to: " + testFile.getAbsolutePath());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save full villager death record: " + e.getMessage());
        } catch (Exception e) {
             plugin.getLogger().warning("Error accessing killer name for log file: " + e.getMessage());
        }
    }
    
    private void notifyPlayers(String killerName, String villagerType, String world, double x, double y, double z) {
        String notifyText = messageManager.get("tracker-notify", 
            "player", killerName,
            "type", villagerType,
            "world", world);
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("newplayerpanel.notify")) {
                try {
                    net.kyori.adventure.text.Component notifyComponent = messageManager.getComponent("tracker-notify",
                        "player", killerName,
                        "type", villagerType,
                        "world", world);
                    net.kyori.adventure.text.Component spaceComponent = net.kyori.adventure.text.Component.text(" ");
                    net.kyori.adventure.text.Component villagerTpComponent = messageManager.createClickableVillagerTpComponent(world, x, y, z);
                    net.kyori.adventure.text.Component fullMessage = notifyComponent.append(spaceComponent).append(villagerTpComponent);
                    
                    java.lang.reflect.Method sendMethod = Player.class.getMethod("sendMessage", net.kyori.adventure.text.Component.class);
                    sendMethod.invoke(onlinePlayer, fullMessage);
                } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException e) {
                    try {
                        net.md_5.bungee.api.chat.TextComponent notifyComponent = new net.md_5.bungee.api.chat.TextComponent(
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(notifyText));
                        net.md_5.bungee.api.chat.TextComponent spaceComponent = new net.md_5.bungee.api.chat.TextComponent(" ");
                        net.md_5.bungee.api.chat.TextComponent villagerTpComponent = messageManager.createClickableVillagerTpComponentSpigot(world, x, y, z);
                        
                        net.md_5.bungee.api.chat.BaseComponent[] fullMessage = new net.md_5.bungee.api.chat.BaseComponent[]{
                            notifyComponent,
                            spaceComponent,
                            villagerTpComponent
                        };
                        
                        onlinePlayer.spigot().sendMessage(fullMessage);
                    } catch (Exception ex) {
                        String coords = String.format("%.0f, %.0f, %.0f", x, y, z);
                        String fullText = notifyText + coords;
                        newplayerpanel.util.ActionBarUtil.sendMessage(onlinePlayer, fullText);
                    }
                }
            }
        }
    }
}