package newplayerpanel.commands;

import newplayerpanel.NewPlayerPanel;
import newplayerpanel.messages.MessageManager;
import newplayerpanel.restrictions.RestrictionsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class NPPCommand implements CommandExecutor, TabCompleter {
    
    private final NewPlayerPanel plugin;
    private final MessageManager messageManager;
    
    private static final List<String> RESTRICTION_TYPES = Arrays.asList("EQUIPMENT", "ITEM", "ENTITY", "COMMAND");
    private static final List<String> ACTIONS = Arrays.asList("DAMAGE", "USE", "DROP", "EQUIP", "EXECUTE");
    
    public NPPCommand(NewPlayerPanel plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "addrestriction":
                return handleAddRestriction(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("newplayerpanel.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        
        plugin.reload();
        sender.sendMessage(messageManager.get("reload-success"));
        return true;
    }
    
    private boolean handleAddRestriction(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newplayerpanel.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(messageManager.get("addrestriction-usage"));
            return true;
        }
        
        String name = args[1];
        String type = args[2].toUpperCase();
        String actionsStr = args[3].toUpperCase();
        
        if (!RESTRICTION_TYPES.contains(type)) {
            sender.sendMessage(messageManager.get("addrestriction-invalid-type", "types", String.join(", ", RESTRICTION_TYPES)));
            return true;
        }
        
        List<String> targets = new ArrayList<>();
        int timeSeconds = -1;
        boolean isDefault = false;
        
        for (int i = 4; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.startsWith("time:")) {
                try {
                    timeSeconds = Integer.parseInt(arg.substring(5));
                } catch (NumberFormatException e) {
                    sender.sendMessage(messageManager.get("invalid-number"));
                    return true;
                }
            } else if (arg.equals("default:true")) {
                isDefault = true;
            } else if (arg.equals("default:false")) {
                isDefault = false;
            } else {
                targets.add(args[i]);
            }
        }
        
        RestrictionsManager restrictionsManager = plugin.getRestrictionsModule().getRestrictionsManager();
        
        boolean success = restrictionsManager.addNewRestriction(name, type, actionsStr, targets, timeSeconds, isDefault);
        
        if (success) {
            sender.sendMessage(messageManager.get("addrestriction-success", "name", name));
        } else {
            sender.sendMessage(messageManager.get("addrestriction-error"));
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messageManager.get("npp-help-header"));
        sender.sendMessage(messageManager.get("npp-help-reload"));
        sender.sendMessage(messageManager.get("npp-help-addrestriction"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "addrestriction");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("addrestriction")) {
            if (args.length == 2) {
            } else if (args.length == 3) {
                String input = args[2].toLowerCase();
                for (String type : RESTRICTION_TYPES) {
                    if (type.toLowerCase().startsWith(input)) {
                        completions.add(type);
                    }
                }
            } else if (args.length == 4) {
                String input = args[3].toLowerCase();
                for (String action : ACTIONS) {
                    if (action.toLowerCase().startsWith(input)) {
                        completions.add(action);
                    }
                }
                if ("damage,use".startsWith(input)) {
                    completions.add("DAMAGE,USE");
                }
                if ("use,drop".startsWith(input)) {
                    completions.add("USE,DROP");
                }
            } else {
                String type = args[2].toUpperCase();
                String lastArg = args[args.length - 1].toLowerCase();
                
                if (type.equals("ITEM") || type.equals("EQUIPMENT")) {
                    if (lastArg.isEmpty() || lastArg.startsWith("minecraft:")) {
                        String[] examples = {"minecraft:diamond_sword", "minecraft:elytra", "minecraft:tnt"};
                        for (String example : examples) {
                            if (example.toLowerCase().startsWith(lastArg)) {
                                completions.add(example);
                            }
                        }
                    }
                } else if (type.equals("ENTITY")) {
                    if (lastArg.isEmpty() || lastArg.startsWith("minecraft:")) {
                        String[] examples = {"minecraft:villager", "minecraft:player"};
                        for (String example : examples) {
                            if (example.toLowerCase().startsWith(lastArg)) {
                                completions.add(example);
                            }
                        }
                    }
                } else if (type.equals("COMMAND")) {
                    String[] examples = {"/tp", "/gamemode"};
                    for (String example : examples) {
                        if (example.toLowerCase().startsWith(lastArg)) {
                            completions.add(example);
                        }
                    }
                }
                
                if (lastArg.isEmpty() || lastArg.startsWith("time:")) {
                    String[] timeOptions = {"time:-1", "time:3600", "time:28800"};
                    for (String option : timeOptions) {
                        if (option.toLowerCase().startsWith(lastArg)) {
                            completions.add(option);
                        }
                    }
                }
                if (lastArg.isEmpty() || lastArg.startsWith("default:")) {
                    String[] defaultOptions = {"default:true", "default:false"};
                    for (String option : defaultOptions) {
                        if (option.toLowerCase().startsWith(lastArg)) {
                            completions.add(option);
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}

