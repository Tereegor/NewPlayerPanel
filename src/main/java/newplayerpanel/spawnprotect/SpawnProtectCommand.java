package newplayerpanel.spawnprotect;

import newplayerpanel.messages.MessageManager;
import newplayerpanel.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnProtectCommand implements CommandExecutor, TabCompleter {

    private static final long CONFIRM_EXPIRE_MS = 30_000;
    private static final double DEFAULT_RADIUS = 100;

    private final SpawnProtectManager manager;
    private final MessageManager messageManager;

    private final Map<UUID, PendingAdd> pendingAdds = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRemove> pendingRemoves = new ConcurrentHashMap<>();

    public SpawnProtectCommand(SpawnProtectManager manager, MessageManager messageManager) {
        this.manager = manager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "info":
                return handleInfo(sender);
            case "playtime":
                return handlePlaytime(sender, args);
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            case "add":
                return handleAdd(sender, args);
            case "remove":
            case "delete":
                return handleRemove(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(messageManager.get("spawnprotect-info-header"));
        sender.sendMessage(messageManager.get("spawnprotect-info-enabled", "enabled",
                manager.isEnabled() ? messageManager.get("spawnprotect-info-yes") : messageManager.get("spawnprotect-info-no")));
        sender.sendMessage(messageManager.get("spawnprotect-info-zones", "count", String.valueOf(manager.getZones().size())));
        long bypass = manager.getBypassAfterPlaytime();
        if (bypass <= 0) {
            sender.sendMessage(messageManager.get("spawnprotect-info-bypass-disabled"));
        } else {
            sender.sendMessage(messageManager.get("spawnprotect-info-bypass", "time", TimeUtil.formatTimeLocalized(bypass, messageManager)));
        }
        return true;
    }

    private boolean handlePlaytime(CommandSender sender, String[] args) {
        Player target;
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messageManager.get("spawnprotect-playtime-usage"));
                return true;
            }
            target = (Player) sender;
        } else {
            if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
                sender.sendMessage(messageManager.get("no-permission"));
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(messageManager.get("player-not-found"));
                return true;
            }
        }

        long seconds = manager.getPlayerPlaytimeSeconds(target);
        String timeStr = TimeUtil.formatTimeLocalized(seconds, messageManager);
        if (target.equals(sender)) {
            sender.sendMessage(messageManager.get("spawnprotect-playtime-self", "time", timeStr));
        } else {
            sender.sendMessage(messageManager.get("spawnprotect-playtime-other", "player", target.getName(), "time", timeStr));
        }
        long remaining = manager.getRemainingTimeForBypass(target);
        if (remaining >= 0) {
            sender.sendMessage(messageManager.get("spawnprotect-playtime-bypass-in", "time", TimeUtil.formatTimeLocalized(remaining, messageManager)));
        } else if (manager.getBypassAfterPlaytime() > 0) {
            sender.sendMessage(messageManager.get("spawnprotect-playtime-bypass-done"));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        manager.reload();
        sender.sendMessage(messageManager.get("spawnprotect-reload-success"));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        Collection<SpawnZone> zones = manager.getZones();
        if (zones.isEmpty()) {
            sender.sendMessage(messageManager.get("spawnprotect-list-empty"));
            return true;
        }
        sender.sendMessage(messageManager.get("spawnprotect-list-header"));
        for (SpawnZone zone : zones) {
            sender.sendMessage(messageManager.get("spawnprotect-list-entry",
                    "name", zone.getName(),
                    "world", zone.getWorldName(),
                    "x", String.format("%.0f", zone.getCenterX()),
                    "z", String.format("%.0f", zone.getCenterZ()),
                    "radius", String.format("%.0f", zone.getRadius())));
        }
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("player-only"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(messageManager.get("spawnprotect-add-usage"));
            return true;
        }

        String name = args[1];
        boolean confirm = args.length >= 3 && "confirm".equalsIgnoreCase(args[2]);

        if (confirm) {
            PendingAdd pending = pendingAdds.remove(player.getUniqueId());
            if (pending == null) {
                sender.sendMessage(messageManager.get("spawnprotect-confirm-expired"));
                return true;
            }
            if (System.currentTimeMillis() - pending.at > CONFIRM_EXPIRE_MS) {
                sender.sendMessage(messageManager.get("spawnprotect-confirm-expired"));
                return true;
            }
            if (!pending.name.equalsIgnoreCase(name)) {
                sender.sendMessage(messageManager.get("spawnprotect-confirm-name-mismatch"));
                return true;
            }
            SpawnZone zone = manager.createDefaultZone(pending.name, pending.worldName,
                    pending.x, pending.z, pending.radius);
            if (manager.addZone(zone)) {
                sender.sendMessage(messageManager.get("spawnprotect-add-success", "name", zone.getName()));
            } else {
                sender.sendMessage(messageManager.get("spawnprotect-add-error-exists", "name", zone.getName()));
            }
            return true;
        }

        double radius = DEFAULT_RADIUS;
        if (args.length >= 3) {
            try {
                radius = Double.parseDouble(args[2]);
                if (radius <= 0 || radius > 10000) {
                    sender.sendMessage(messageManager.get("spawnprotect-invalid-radius"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(messageManager.get("invalid-number"));
                return true;
            }
        }

        if (manager.getZone(name) != null) {
            sender.sendMessage(messageManager.get("spawnprotect-add-error-exists", "name", name));
            return true;
        }

        Location loc = player.getLocation();
        pendingAdds.put(player.getUniqueId(), new PendingAdd(name, loc.getWorld().getName(),
                loc.getBlockX() + 0.5, loc.getBlockZ() + 0.5, radius, System.currentTimeMillis()));
        sender.sendMessage(messageManager.get("spawnprotect-add-confirm", "name", name));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageManager.get("spawnprotect-remove-usage"));
            return true;
        }

        String name = args[1];
        boolean confirm = args.length >= 3 && "confirm".equalsIgnoreCase(args[2]);

        if (confirm) {
            if (sender instanceof Player) {
                PendingRemove pending = pendingRemoves.remove(((Player) sender).getUniqueId());
                if (pending == null) {
                    sender.sendMessage(messageManager.get("spawnprotect-confirm-expired"));
                    return true;
                }
                if (System.currentTimeMillis() - pending.at > CONFIRM_EXPIRE_MS) {
                    sender.sendMessage(messageManager.get("spawnprotect-confirm-expired"));
                    return true;
                }
                if (!pending.name.equalsIgnoreCase(name)) {
                    sender.sendMessage(messageManager.get("spawnprotect-confirm-name-mismatch"));
                    return true;
                }
            }
            if (manager.removeZone(name)) {
                sender.sendMessage(messageManager.get("spawnprotect-remove-success", "name", name));
            } else {
                sender.sendMessage(messageManager.get("spawnprotect-remove-error-not-found", "name", name));
            }
            return true;
        }

        if (manager.getZone(name) == null) {
            sender.sendMessage(messageManager.get("spawnprotect-remove-error-not-found", "name", name));
            return true;
        }

        if (sender instanceof Player) {
            pendingRemoves.put(((Player) sender).getUniqueId(),
                    new PendingRemove(name, System.currentTimeMillis()));
        } else {
            sender.sendMessage(messageManager.get("spawnprotect-console-must-confirm"));
            return true;
        }
        sender.sendMessage(messageManager.get("spawnprotect-remove-confirm", "name", name));
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(messageManager.get("spawnprotect-usage"));
        if (sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("spawnprotect-usage-admin"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("info", "playtime"));
            if (sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
                subs.addAll(Arrays.asList("list", "add", "remove", "reload"));
            }
            String input = args[0].toLowerCase();
            for (String sub : subs) {
                if (sub.startsWith(input)) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("playtime".equals(sub) && sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        out.add(p.getName());
                    }
                }
            } else if ("add".equals(sub)) {
                if ("<name>".startsWith(args[1].toLowerCase()) || args[1].isEmpty()) {
                    out.add("<name>");
                }
            } else if ("remove".equals(sub) || "delete".equals(sub)) {
                for (SpawnZone zone : manager.getZones()) {
                    if (zone.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        out.add(zone.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            if ("add".equalsIgnoreCase(args[0])) {
                if ("confirm".startsWith(args[2].toLowerCase())) {
                    out.add("confirm");
                }
                if (args[2].matches("[0-9.]*")) {
                    out.add("50");
                    out.add("100");
                }
            } else if (("remove".equalsIgnoreCase(args[0]) || "delete".equalsIgnoreCase(args[0]))
                    && manager.getZone(args[1]) != null) {
                if ("confirm".startsWith(args[2].toLowerCase())) {
                    out.add("confirm");
                }
            }
        }
        return out;
    }

    private static class PendingAdd {
        final String name;
        final String worldName;
        final double x, z;
        final double radius;
        final long at;

        PendingAdd(String name, String worldName, double x, double z, double radius, long at) {
            this.name = name;
            this.worldName = worldName;
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.at = at;
        }
    }

    private static class PendingRemove {
        final String name;
        final long at;

        PendingRemove(String name, long at) {
            this.name = name;
            this.at = at;
        }
    }
}
