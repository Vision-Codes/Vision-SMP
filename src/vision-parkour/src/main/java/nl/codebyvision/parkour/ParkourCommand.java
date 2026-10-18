package nl.codebyvision.parkour;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class ParkourCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_ACTIONS = List.of("checkpoint", "leave", "top", "list");
    private static final List<String> ADMIN_ACTIONS =
            List.of("create", "setstart", "addcheckpoint", "removecheckpoint", "setfinish", "delete", "reload");

    private final VisionParkour plugin;

    ParkourCommand(VisionParkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (ADMIN_ACTIONS.contains(action)) {
            if (!sender.hasPermission("parkour.admin")) {
                sender.sendMessage(plugin.message("no-permission"));
            } else {
                admin(sender, action, args);
            }
            return true;
        }
        switch (action) {
            case "checkpoint" -> {
                VisionParkour.Run run = sender instanceof Player p ? plugin.runs().get(p.getUniqueId()) : null;
                if (run == null) {
                    sender.sendMessage(plugin.message("not-running"));
                } else {
                    plugin.toCheckpoint((Player) sender, run);
                }
            }
            case "leave" -> {
                if (sender instanceof Player player && plugin.runs().containsKey(player.getUniqueId())) {
                    plugin.stop(player);
                    sender.sendMessage(plugin.message("left"));
                } else {
                    sender.sendMessage(plugin.message("not-running"));
                }
            }
            case "top" -> top(sender, args);
            case "list" -> {
                if (plugin.courses().all().isEmpty()) {
                    sender.sendMessage(plugin.message("no-courses"));
                }
                for (Course course : plugin.courses().all().values()) {
                    sender.sendMessage(plugin.message(course.isReady() ? "course-entry" : "course-entry-unfinished",
                            "course", course.name, "checkpoints", String.valueOf(course.checkpoints.size())));
                }
            }
            default -> sender.sendMessage(plugin.message(sender.hasPermission("parkour.admin") ? "usage-admin" : "usage"));
        }
        return true;
    }

    private void top(CommandSender sender, String[] args) {
        Course course = args.length > 1 ? plugin.courses().get(args[1]) : null;
        if (course == null) {
            sender.sendMessage(plugin.message("unknown-course", "course", args.length > 1 ? args[1] : "?"));
            return;
        }
        List<CourseManager.Time> top = plugin.courses().top(course.name, 10);
        sender.sendMessage(plugin.message("top-header", "course", course.name));
        if (top.isEmpty()) {
            sender.sendMessage(plugin.message("top-empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            sender.sendMessage(plugin.message("top-entry", "rank", String.valueOf(i + 1),
                    "player", top.get(i).name(), "time", VisionParkour.format(top.get(i).millis())));
        }
    }

    private void admin(CommandSender sender, String action, String[] args) {
        if (action.equals("reload")) {
            plugin.reloadConfig();
            plugin.courses().load();
            sender.sendMessage(plugin.message("reloaded"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.message("usage-admin"));
            return;
        }
        String name = args[1];
        if (action.equals("create")) {
            if (plugin.courses().get(name) != null) {
                sender.sendMessage(plugin.message("course-exists", "course", name));
            } else {
                plugin.courses().create(name);
                sender.sendMessage(plugin.message("course-created", "course", name));
            }
            return;
        }
        Course course = plugin.courses().get(name);
        if (course == null) {
            sender.sendMessage(plugin.message("unknown-course", "course", name));
            return;
        }
        if (action.equals("delete")) {
            plugin.courses().delete(name);
            sender.sendMessage(plugin.message("course-deleted", "course", name));
            return;
        }
        if (action.equals("removecheckpoint")) {
            if (!course.checkpoints.isEmpty()) {
                course.checkpoints.removeLast();
                plugin.courses().saveCourses();
            }
            sender.sendMessage(plugin.message("checkpoint-removed", "course", name,
                    "checkpoints", String.valueOf(course.checkpoints.size())));
            return;
        }

        // setstart, addcheckpoint, setfinish: the pressure plate you are standing on
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("players-only"));
            return;
        }
        Block block = player.getLocation().getBlock();
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) {
            sender.sendMessage(plugin.message("stand-on-plate"));
            return;
        }
        Location plate = block.getLocation();
        switch (action) {
            case "setstart" -> course.start = plate;
            case "addcheckpoint" -> course.checkpoints.add(plate);
            default -> course.finish = plate;
        }
        plugin.courses().saveCourses();
        sender.sendMessage(plugin.message("plate-set", "what", action, "course", name,
                "checkpoints", String.valueOf(course.checkpoints.size())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        boolean admin = sender.hasPermission("parkour.admin");
        if (args.length == 1) {
            return admin
                    ? Stream.concat(PLAYER_ACTIONS.stream(), ADMIN_ACTIONS.stream()).toList()
                    : PLAYER_ACTIONS;
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")
                && (args[0].equalsIgnoreCase("top") || admin && ADMIN_ACTIONS.contains(args[0].toLowerCase(Locale.ROOT)))) {
            return List.copyOf(plugin.courses().all().keySet());
        }
        return List.of();
    }
}
