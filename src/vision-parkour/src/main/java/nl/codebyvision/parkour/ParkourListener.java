package nl.codebyvision.parkour;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

final class ParkourListener implements Listener {

    private final VisionParkour plugin;

    ParkourListener(VisionParkour plugin) {
        this.plugin = plugin;
    }

    /** Stepping on a pressure plate (fires every tick you stand on it, so everything here is safe to repeat). */
    @EventHandler
    public void onPlate(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.PHYSICAL || block == null) {
            return;
        }
        Player player = event.getPlayer();
        Location plate = block.getLocation();
        VisionParkour.Run run = plugin.runs().get(player.getUniqueId());

        for (Course course : plugin.courses().all().values()) {
            if (!course.isReady()) {
                continue;
            }
            if (plate.equals(course.start)) {
                if (run != null && run.course == course && run.checkpoint < 0) {
                    // Still on (or back at) the start: the clock starts when you step off
                    run.startedAt = System.currentTimeMillis();
                } else {
                    plugin.runs().put(player.getUniqueId(), new VisionParkour.Run(course));
                    player.sendMessage(plugin.message("started", "course", course.name));
                }
                return;
            }
            if (run == null || run.course != course) {
                continue;
            }
            int index = course.checkpoints.indexOf(plate);
            if (index > run.checkpoint) {
                if (index != run.checkpoint + 1) {
                    if (run.mayWarn()) {
                        player.sendMessage(plugin.message("skipped-checkpoint"));
                    }
                    return;
                }
                run.checkpoint = index;
                run.respawn = plate;
                player.sendMessage(plugin.message("checkpoint", "number", String.valueOf(index + 1),
                        "total", String.valueOf(course.checkpoints.size()), "time", VisionParkour.format(run.elapsed())));
                return;
            }
            if (plate.equals(course.finish)) {
                finish(player, run);
                return;
            }
        }
    }

    private void finish(Player player, VisionParkour.Run run) {
        Course course = run.course;
        if (run.checkpoint != course.checkpoints.size() - 1) {
            if (run.mayWarn()) {
                player.sendMessage(plugin.message("missed-checkpoints"));
            }
            return;
        }
        long time = run.elapsed();
        plugin.stop(player);

        boolean first = !plugin.courses().hasFinished(course.name, player.getUniqueId());
        boolean personalBest = plugin.courses().record(course.name, player.getUniqueId(), player.getName(), time);
        player.sendMessage(plugin.message(personalBest ? "finished-best" : "finished",
                "course", course.name, "time", VisionParkour.format(time)));

        List<CourseManager.Time> top = plugin.courses().top(course.name, 1);
        if (personalBest && !top.isEmpty() && top.getFirst().player().equals(player.getUniqueId())) {
            Bukkit.broadcast(plugin.message("record", "player", player.getName(), "course", course.name,
                    "time", VisionParkour.format(time)));
        }
        if (first) {
            for (String command : plugin.getConfig().getStringList("first-completion-commands")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("%player%", player.getName()).replace("%course%", course.name));
            }
        }
    }

    /** Falling too far below the last plate puts you back on it. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        VisionParkour.Run run = plugin.runs().get(event.getPlayer().getUniqueId());
        if (run == null || !event.hasChangedBlock()) {
            return;
        }
        if (event.getTo().getY() < run.respawn.getY() - plugin.getConfig().getDouble("fall-depth", 5)) {
            plugin.toCheckpoint(event.getPlayer(), run);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            plugin.stop(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.stop(event.getPlayer());
    }
}
