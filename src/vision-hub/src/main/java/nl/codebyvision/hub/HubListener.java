package nl.codebyvision.hub;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

final class HubListener implements Listener {

    private static final long PORTAL_COOLDOWN_MS = 3000;

    private final VisionHub plugin;
    private final Map<UUID, Long> lastPortal = new HashMap<>();

    HubListener(VisionHub plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("join-at-hub")) {
            return;
        }
        Location spawn = plugin.hubSpawn();
        if (spawn != null) {
            // Next tick, so other plugins that move players on join do not override it
            plugin.getServer().getScheduler().runTask(plugin, () -> event.getPlayer().teleportAsync(spawn));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastPortal.remove(event.getPlayer().getUniqueId());
    }

    /** Switching to another mode swaps inventory, XP, health and gamemode. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        World from = event.getFrom().getWorld();
        World to = event.getTo().getWorld();
        if (from == null || to == null || from.equals(to)) {
            return;
        }
        String fromGroup = plugin.groupOf(from);
        String toGroup = plugin.groupOf(to);
        if (fromGroup.equals(toGroup)) {
            return;
        }
        Player player = event.getPlayer();
        plugin.inventories().save(player, fromGroup, event.getFrom());
        plugin.inventories().load(player, toGroup);

        Mode mode = plugin.modeOf(to);
        if (mode != null && !player.hasPermission("hub.keepgamemode")) {
            player.setGameMode(mode.gameMode());
        }
    }

    /** Respawning must stay inside the mode you died in, otherwise inventories would get mixed up. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Mode mode = plugin.modeOf(event.getPlayer().getWorld());
        if (mode == null || mode.equals(plugin.modeOf(event.getRespawnLocation().getWorld()))) {
            return;
        }
        World world = plugin.getServer().getWorld(mode.worlds().getFirst());
        if (world != null) {
            event.setRespawnLocation(world.getSpawnLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        PortalManager.Portal portal = plugin.portals().at(event.getTo());
        if (portal == null) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (now - lastPortal.getOrDefault(player.getUniqueId(), 0L) < PORTAL_COOLDOWN_MS) {
            return;
        }
        lastPortal.put(player.getUniqueId(), now);

        Mode mode = plugin.modes().get(portal.mode());
        Location target = mode != null ? plugin.entryPoint(player.getUniqueId(), mode) : null;
        if (target == null) {
            player.sendMessage(plugin.message("mode-unavailable", "mode", portal.mode()));
            return;
        }
        player.teleportAsync(target);
        player.sendMessage(plugin.message("mode-joined", "mode", mode.name()));
    }

    // Protected modes (hub, parkour): no building, no damage, no hunger

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isProtectedWorld(player.getWorld())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            player.teleportAsync(player.getWorld().getSpawnLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isProtectedWorld(player.getWorld())) {
            event.setCancelled(true);
        }
    }

    private boolean isProtected(Player player) {
        return isProtectedWorld(player.getWorld()) && !player.hasPermission("hub.build");
    }

    private boolean isProtectedWorld(World world) {
        Mode mode = plugin.modeOf(world);
        return mode != null && mode.protectedWorld();
    }
}
