package nl.codebyvision.hub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class HubCommand implements CommandExecutor, TabCompleter {

    private final VisionHub plugin;
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    HubCommand(VisionHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.message("players-only"));
                return true;
            }
            sendToHub(player);
            return true;
        }
        if (!sender.hasPermission("hub.admin")) {
            sender.sendMessage(plugin.message("no-permission"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.message("reloaded"));
            }
            case "pos1", "pos2" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.message("players-only"));
                    return true;
                }
                Location location = player.getLocation().getBlock().getLocation();
                (args[0].equalsIgnoreCase("pos1") ? pos1 : pos2).put(player.getUniqueId(), location);
                sender.sendMessage(plugin.message("pos-set", "pos", args[0].toLowerCase(),
                        "x", String.valueOf(location.getBlockX()), "y", String.valueOf(location.getBlockY()),
                        "z", String.valueOf(location.getBlockZ())));
            }
            case "portal" -> portal(sender, args);
            default -> sender.sendMessage(plugin.message("usage"));
        }
        return true;
    }

    private void sendToHub(Player player) {
        Location spawn = plugin.hubSpawn();
        if (spawn == null) {
            player.sendMessage(plugin.message("no-hub"));
            return;
        }
        player.teleportAsync(spawn);
    }

    private void portal(CommandSender sender, String[] args) {
        if (args.length >= 4 && args[1].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.message("players-only"));
                return;
            }
            Location a = pos1.get(player.getUniqueId());
            Location b = pos2.get(player.getUniqueId());
            if (a == null || b == null || !a.getWorld().equals(b.getWorld())) {
                sender.sendMessage(plugin.message("select-first"));
                return;
            }
            if (!plugin.modes().containsKey(args[3])) {
                sender.sendMessage(plugin.message("unknown-mode", "mode", args[3]));
                return;
            }
            plugin.portals().create(args[2], args[3], a, b);
            sender.sendMessage(plugin.message("portal-created", "name", args[2], "mode", args[3]));
        } else if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
            sender.sendMessage(plugin.portals().remove(args[2])
                    ? plugin.message("portal-removed", "name", args[2])
                    : plugin.message("unknown-portal", "name", args[2]));
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            if (plugin.portals().all().isEmpty()) {
                sender.sendMessage(plugin.message("no-portals"));
            }
            plugin.portals().all().values().forEach(portal -> sender.sendMessage(plugin.message("portal-entry",
                    "name", portal.name(), "mode", portal.mode(), "world", portal.world())));
        } else {
            sender.sendMessage(plugin.message("usage"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hub.admin")) {
            return List.of();
        }
        return switch (args.length) {
            case 1 -> List.of("pos1", "pos2", "portal", "reload");
            case 2 -> args[0].equalsIgnoreCase("portal") ? List.of("create", "remove", "list") : List.of();
            case 3 -> args[1].equalsIgnoreCase("remove") ? List.copyOf(plugin.portals().all().keySet()) : List.of();
            case 4 -> args[1].equalsIgnoreCase("create") ? List.copyOf(plugin.modes().keySet()) : List.of();
            default -> List.of();
        };
    }
}
