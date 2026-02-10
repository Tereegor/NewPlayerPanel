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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnProtectCommand implements CommandExecutor, TabCompleter, Listener {

    private static final long CONFIRM_EXPIRE_MS = 30_000;
    private static final double DEFAULT_RADIUS = 100;

    private final SpawnProtectManager manager;
    private final MessageManager messageManager;

    private final Map<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final Map<UUID, double[]> pos1 = new ConcurrentHashMap<>();
    private final Map<UUID, double[]> pos2 = new ConcurrentHashMap<>();

    private static final List<String> EDIT_KEYS = Arrays.asList(
            "radius", "center", "pvp", "explosions", "fire-spread", "world");
    private static final List<String> BOOL_VALUES = Arrays.asList("true", "false");

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
            case "info":      return handleInfo(sender);
            case "playtime":  return handlePlaytime(sender, args);
            case "reload":    return handleReload(sender);
            case "list":      return handleList(sender);
            case "add":       return handleAdd(sender, args);
            case "remove":
            case "delete":    return handleRemove(sender, args);
            case "edit":      return handleEdit(sender, args);
            case "pos1":      return handlePos(sender, 1);
            case "pos2":      return handlePos(sender, 2);
            case "addpoint":  return handleAddPoint(sender, args);
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
                    "shape", zone.getShapeDescription()));
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

        for (int i = 2; i < args.length; i++) {
            if ("confirm".equalsIgnoreCase(args[i])) {
                return confirmAdd(player, name);
            }
        }

        String typeArg = args.length >= 3 ? args[2].toLowerCase() : "";

        if ("rect".equals(typeArg)) {
            return startAddRect(player, name);
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
        PendingAction pending = new PendingAction(PendingAction.Type.ADD_CIRCLE, name);
        pending.worldName = loc.getWorld().getName();
        pending.x = loc.getBlockX() + 0.5;
        pending.z = loc.getBlockZ() + 0.5;
        pending.radius = radius;
        pendingActions.put(player.getUniqueId(), pending);

        sender.sendMessage(messageManager.get("spawnprotect-add-confirm", "name", name));
        return true;
    }

    private boolean startAddRect(Player player, String name) {
        if (manager.getZone(name) != null) {
            player.sendMessage(messageManager.get("spawnprotect-add-error-exists", "name", name));
            return true;
        }
        double[] p1 = pos1.get(player.getUniqueId());
        double[] p2 = pos2.get(player.getUniqueId());
        if (p1 == null || p2 == null) {
            player.sendMessage(messageManager.get("spawnprotect-add-rect-need-pos"));
            return true;
        }

        PendingAction pending = new PendingAction(PendingAction.Type.ADD_RECT, name);
        pending.worldName = player.getWorld().getName();
        pending.minX = Math.min(p1[0], p2[0]);
        pending.minZ = Math.min(p1[1], p2[1]);
        pending.maxX = Math.max(p1[0], p2[0]);
        pending.maxZ = Math.max(p1[1], p2[1]);
        pendingActions.put(player.getUniqueId(), pending);

        player.sendMessage(messageManager.get("spawnprotect-add-rect-confirm", "name", name,
                "min", String.format("%.0f, %.0f", pending.minX, pending.minZ),
                "max", String.format("%.0f, %.0f", pending.maxX, pending.maxZ)));
        return true;
    }

    private boolean confirmAdd(Player player, String name) {
        PendingAction pending = pendingActions.remove(player.getUniqueId());
        if (pending == null || System.currentTimeMillis() - pending.at > CONFIRM_EXPIRE_MS) {
            player.sendMessage(messageManager.get("spawnprotect-confirm-expired"));
            return true;
        }
        if (!pending.name.equalsIgnoreCase(name)) {
            player.sendMessage(messageManager.get("spawnprotect-confirm-name-mismatch"));
            return true;
        }

        SpawnZone zone;
        switch (pending.type) {
            case ADD_RECT:
                zone = manager.createDefaultRect(pending.name, pending.worldName,
                        pending.minX, pending.minZ, pending.maxX, pending.maxZ);
                break;
            default:
                zone = manager.createDefaultCircle(pending.name, pending.worldName,
                        pending.x, pending.z, pending.radius);
                break;
        }
        if (manager.addZone(zone)) {
            player.sendMessage(messageManager.get("spawnprotect-add-success", "name", zone.getName()));
        } else {
            player.sendMessage(messageManager.get("spawnprotect-add-error-exists", "name", zone.getName()));
        }
        return true;
    }

    private boolean handlePos(CommandSender sender, int which) {
        if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("player-only"));
            return true;
        }
        Player player = (Player) sender;
        Location loc = player.getLocation();
        double[] coords = new double[]{loc.getBlockX(), loc.getBlockZ()};
        if (which == 1) {
            pos1.put(player.getUniqueId(), coords);
        } else {
            pos2.put(player.getUniqueId(), coords);
        }
        sender.sendMessage(messageManager.get("spawnprotect-pos-set", "pos", String.valueOf(which),
                "x", String.format("%.0f", coords[0]), "z", String.format("%.0f", coords[1])));
        return true;
    }

    private boolean handleAddPoint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("player-only"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messageManager.get("spawnprotect-addpoint-usage"));
            return true;
        }
        Player player = (Player) sender;
        String name = args[1];
        SpawnZone zone = manager.getZone(name);
        if (zone == null) {
            List<double[]> pts = new ArrayList<>();
            pts.add(new double[]{player.getLocation().getBlockX(), player.getLocation().getBlockZ()});
            SpawnZone newZone = manager.createDefaultPoly(name, player.getWorld().getName(), pts);
            if (manager.addZone(newZone)) {
                sender.sendMessage(messageManager.get("spawnprotect-addpoint-created", "name", name, "index", "0"));
            } else {
                sender.sendMessage(messageManager.get("spawnprotect-add-error-exists", "name", name));
            }
            return true;
        }
        if (zone.getShapeType() != SpawnZone.ShapeType.POLY) {
            sender.sendMessage(messageManager.get("spawnprotect-addpoint-not-poly", "name", name));
            return true;
        }
        zone.addPoint(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
        manager.saveZone(zone);
        sender.sendMessage(messageManager.get("spawnprotect-addpoint-added", "name", name,
                "index", String.valueOf(zone.getPoints().size() - 1),
                "total", String.valueOf(zone.getPoints().size())));
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
                PendingAction pending = pendingActions.remove(((Player) sender).getUniqueId());
                if (pending == null || pending.type != PendingAction.Type.REMOVE) {
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
            } else if (manager.getZone(name) == null) {
                sender.sendMessage(messageManager.get("spawnprotect-remove-error-not-found", "name", name));
                return true;
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
            pendingActions.put(((Player) sender).getUniqueId(),
                    new PendingAction(PendingAction.Type.REMOVE, name));
            sender.sendMessage(messageManager.get("spawnprotect-remove-confirm", "name", name));
        } else {
            sender.sendMessage(messageManager.get("spawnprotect-console-must-confirm"));
        }
        return true;
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newplayerpanel.spawnprotect.admin")) {
            sender.sendMessage(messageManager.get("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-usage"));
            return true;
        }

        String zoneName = args[1];
        SpawnZone zone = manager.getZone(zoneName);
        if (zone == null) {
            sender.sendMessage(messageManager.get("spawnprotect-remove-error-not-found", "name", zoneName));
            return true;
        }

        String key = args[2].toLowerCase();
        String value = args.length >= 4 ? args[3] : null;

        switch (key) {
            case "radius":
                return editRadius(sender, zone, value);
            case "center":
                return editCenter(sender, zone);
            case "pvp":
                return editBool(sender, zone, key, value);
            case "explosions":
                return editBool(sender, zone, key, value);
            case "fire-spread":
                return editBool(sender, zone, key, value);
            case "world":
                return editWorld(sender, zone, value);
            case "removepoint":
                return editRemovePoint(sender, zone, value);
            default:
                sender.sendMessage(messageManager.get("spawnprotect-edit-unknown-key", "key", key));
                return true;
        }
    }

    private boolean editRadius(CommandSender sender, SpawnZone zone, String value) {
        if (zone.getShapeType() != SpawnZone.ShapeType.CIRCLE) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-circle-only"));
            return true;
        }
        if (value == null) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-radius-usage"));
            return true;
        }
        try {
            double r = Double.parseDouble(value);
            if (r <= 0 || r > 10000) {
                sender.sendMessage(messageManager.get("spawnprotect-invalid-radius"));
                return true;
            }
            zone.setRadius(r);
            manager.saveZone(zone);
            sender.sendMessage(messageManager.get("spawnprotect-edit-success", "name", zone.getName(),
                    "key", "radius", "value", String.format("%.0f", r)));
        } catch (NumberFormatException e) {
            sender.sendMessage(messageManager.get("invalid-number"));
        }
        return true;
    }

    private boolean editCenter(CommandSender sender, SpawnZone zone) {
        if (zone.getShapeType() != SpawnZone.ShapeType.CIRCLE) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-circle-only"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("player-only"));
            return true;
        }
        Player player = (Player) sender;
        zone.setCenterX(player.getLocation().getBlockX() + 0.5);
        zone.setCenterZ(player.getLocation().getBlockZ() + 0.5);
        manager.saveZone(zone);
        sender.sendMessage(messageManager.get("spawnprotect-edit-success", "name", zone.getName(),
                "key", "center", "value", String.format("%.0f, %.0f", zone.getCenterX(), zone.getCenterZ())));
        return true;
    }

    private boolean editBool(CommandSender sender, SpawnZone zone, String key, String value) {
        if (value == null || (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-bool-usage", "key", key));
            return true;
        }
        boolean b = Boolean.parseBoolean(value);
        switch (key) {
            case "pvp":         zone.setPvpEnabled(b); break;
            case "explosions":  zone.setExplosionsEnabled(b); break;
            case "fire-spread": zone.setFireSpreadEnabled(b); break;
        }
        manager.saveZone(zone);
        sender.sendMessage(messageManager.get("spawnprotect-edit-success", "name", zone.getName(),
                "key", key, "value", String.valueOf(b)));
        return true;
    }

    private boolean editWorld(CommandSender sender, SpawnZone zone, String value) {
        if (value == null || value.isEmpty()) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-world-usage"));
            return true;
        }
        if (Bukkit.getWorld(value) == null) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-world-not-found", "world", value));
            return true;
        }
        zone.setWorldName(value);
        manager.saveZone(zone);
        sender.sendMessage(messageManager.get("spawnprotect-edit-success", "name", zone.getName(),
                "key", "world", "value", value));
        return true;
    }

    private boolean editRemovePoint(CommandSender sender, SpawnZone zone, String value) {
        if (zone.getShapeType() != SpawnZone.ShapeType.POLY) {
            sender.sendMessage(messageManager.get("spawnprotect-addpoint-not-poly", "name", zone.getName()));
            return true;
        }
        if (value == null) {
            sender.sendMessage(messageManager.get("spawnprotect-edit-removepoint-usage"));
            return true;
        }
        try {
            int index = Integer.parseInt(value);
            if (zone.getPoints().size() <= 3) {
                sender.sendMessage(messageManager.get("spawnprotect-edit-removepoint-min"));
                return true;
            }
            if (!zone.removePoint(index)) {
                sender.sendMessage(messageManager.get("spawnprotect-edit-removepoint-invalid", "max", String.valueOf(zone.getPoints().size() - 1)));
                return true;
            }
            manager.saveZone(zone);
            sender.sendMessage(messageManager.get("spawnprotect-edit-success", "name", zone.getName(),
                    "key", "removepoint", "value", String.valueOf(index)));
        } catch (NumberFormatException e) {
            sender.sendMessage(messageManager.get("invalid-number"));
        }
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
        boolean admin = sender.hasPermission("newplayerpanel.spawnprotect.admin");

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("info", "playtime"));
            if (admin) {
                subs.addAll(Arrays.asList("list", "add", "remove", "edit", "reload", "pos1", "pos2", "addpoint"));
            }
            filter(subs, args[0], out);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "playtime":
                    if (admin) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                                out.add(p.getName());
                        }
                    }
                    break;
                case "add":
                case "addpoint":
                    break;
                case "remove":
                case "delete":
                case "edit":
                    if (admin) {
                        for (SpawnZone z : manager.getZones()) {
                            if (z.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                                out.add(z.getName());
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ("add".equals(sub)) {
                filter(Arrays.asList("confirm", "rect"), args[2], out);
                if (args[2].matches("[0-9.]*")) {
                    out.add("50");
                    out.add("100");
                }
            } else if ("remove".equals(sub) || "delete".equals(sub)) {
                filter(Collections.singletonList("confirm"), args[2], out);
            } else if ("edit".equals(sub) && admin) {
                SpawnZone z = manager.getZone(args[1]);
                List<String> keys = new ArrayList<>(EDIT_KEYS);
                if (z != null && z.getShapeType() == SpawnZone.ShapeType.POLY) {
                    keys.add("removepoint");
                }
                filter(keys, args[2], out);
            }
        } else if (args.length == 4 && "edit".equalsIgnoreCase(args[0]) && admin) {
            String key = args[2].toLowerCase();
            if ("pvp".equals(key) || "explosions".equals(key) || "fire-spread".equals(key)) {
                filter(BOOL_VALUES, args[3], out);
            } else if ("world".equals(key)) {
                for (org.bukkit.World w : Bukkit.getWorlds()) {
                    if (w.getName().toLowerCase().startsWith(args[3].toLowerCase()))
                        out.add(w.getName());
                }
            }
        }
        return out;
    }

    private void filter(List<String> options, String input, List<String> out) {
        String lower = input.toLowerCase();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pos1.remove(uuid);
        pos2.remove(uuid);
    }

    private static class PendingAction {
        enum Type { ADD_CIRCLE, ADD_RECT, REMOVE }

        final Type type;
        final String name;
        final long at;

        String worldName;
        double x, z, radius;
        double minX, minZ, maxX, maxZ;

        PendingAction(Type type, String name) {
            this.type = type;
            this.name = name;
            this.at = System.currentTimeMillis();
        }
    }
}
