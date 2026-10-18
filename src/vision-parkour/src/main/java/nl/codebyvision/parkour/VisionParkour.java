package nl.codebyvision.parkour;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class VisionParkour extends JavaPlugin {

    /** A player's attempt: which course, when they started and the last plate they reached. */
    static final class Run {
        final Course course;
        long startedAt = System.currentTimeMillis();
        int checkpoint = -1;
        Location respawn;
        private long lastWarning;

        Run(Course course) {
            this.course = course;
            this.respawn = course.start;
        }

        long elapsed() {
            return System.currentTimeMillis() - startedAt;
        }

        /** Plates fire every tick you stand on them; this keeps warnings to one per 2 seconds. */
        boolean mayWarn() {
            long now = System.currentTimeMillis();
            if (now - lastWarning < 2000) {
                return false;
            }
            lastWarning = now;
            return true;
        }
    }

    private final Map<UUID, Run> runs = new HashMap<>();
    private CourseManager courses;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        courses = new CourseManager(this);

        ParkourCommand command = new ParkourCommand(this);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("parkour"), "command missing in plugin.yml");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new ParkourListener(this), this);

        // Running timer in the action bar
        getServer().getScheduler().runTaskTimer(this, () -> runs.forEach((id, run) -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendActionBar(MiniMessage.miniMessage().deserialize(
                        getConfig().getString("messages.timer", "<time>").replace("<time>", format(run.elapsed()))));
            }
        }), 2, 2);
    }

    CourseManager courses() {
        return courses;
    }

    Map<UUID, Run> runs() {
        return runs;
    }

    void stop(Player player) {
        if (runs.remove(player.getUniqueId()) != null) {
            player.sendActionBar(Component.empty());
        }
    }

    /** Teleports to the middle of the last reached plate, facing the way the player looks. */
    void toCheckpoint(Player player, Run run) {
        Location target = run.respawn.clone().add(0.5, 0, 0.5);
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.setFallDistance(0);
        player.teleportAsync(target);
    }

    static String format(long millis) {
        return String.format("%d:%02d.%03d", millis / 60000, (millis / 1000) % 60, millis % 1000);
    }

    /** Message from config.yml (messages.<key>) with the prefix in front, in MiniMessage format. */
    public Component message(String key, String... placeholders) {
        String prefix = getConfig().getString("messages.prefix", "");
        String text = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
        }
        return MiniMessage.miniMessage().deserialize(prefix + text);
    }
}
