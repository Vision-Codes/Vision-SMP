package nl.codebyvision.hub;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

public final class VisionHub extends JavaPlugin {

    private final Map<String, Mode> modes = new LinkedHashMap<>();
    private InventoryStore inventories;
    private PortalManager portals;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadModes();
        inventories = new InventoryStore(this);
        portals = new PortalManager(this);

        HubCommand command = new HubCommand(this);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("hub"), "command missing in plugin.yml");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new HubListener(this), this);
        getServer().getScheduler().runTaskTimer(this, this::showPortals, 20, 10);
    }

    /** Particles in every portal opening, so players can see where to walk through. */
    private void showPortals() {
        if (!getConfig().getBoolean("portal-particles", true)) {
            return;
        }
        for (PortalManager.Portal portal : portals.all().values()) {
            World world = Bukkit.getWorld(portal.world());
            if (world == null || world.getPlayers().isEmpty()) {
                continue;
            }
            BoundingBox box = portal.box();
            Location center = box.getCenter().toLocation(world);
            int count = (int) Math.min(40, Math.max(4, box.getVolume() * 2));
            world.spawnParticle(Particle.PORTAL, center, count, box.getWidthX() / 3, box.getHeight() / 3, box.getWidthZ() / 3, 0.2);
        }
    }

    void reload() {
        reloadConfig();
        loadModes();
        portals.load();
    }

    private void loadModes() {
        modes.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("modes");
        if (section == null) {
            return;
        }
        for (String name : section.getKeys(false)) {
            ConfigurationSection mode = Objects.requireNonNull(section.getConfigurationSection(name));
            List<String> worlds = new ArrayList<>(mode.getStringList("worlds"));
            if (worlds.isEmpty()) {
                getLogger().warning("Mode " + name + " has no worlds, skipped.");
                continue;
            }
            GameMode gameMode;
            try {
                gameMode = GameMode.valueOf(mode.getString("gamemode", "survival").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Mode " + name + " has an unknown gamemode, using survival.");
                gameMode = GameMode.SURVIVAL;
            }
            modes.put(name, new Mode(name, worlds, gameMode, mode.getBoolean("protected")));
        }
    }

    Map<String, Mode> modes() {
        return modes;
    }

    /** The mode a world belongs to, or null when it is not listed in config.yml. */
    Mode modeOf(World world) {
        for (Mode mode : modes.values()) {
            if (mode.worlds().contains(world.getName())) {
                return mode;
            }
        }
        return null;
    }

    /** Inventory group of a world: its mode, or the world itself when it has no mode. */
    String groupOf(World world) {
        Mode mode = modeOf(world);
        return mode != null ? mode.name() : "world:" + world.getName();
    }

    /** Where a player enters a mode: the last spot they left it, otherwise the spawn of its first world. */
    Location entryPoint(UUID player, Mode mode) {
        Location last = inventories.lastLocation(player, mode.name());
        if (last != null && last.getWorld() != null) {
            return last;
        }
        World world = Bukkit.getWorld(mode.worlds().getFirst());
        return world != null ? world.getSpawnLocation() : null;
    }

    /** Spawn of the first world of the hub mode, or null when that world does not exist. */
    Location hubSpawn() {
        Mode hub = modes.get(getConfig().getString("hub-mode", "hub"));
        World world = hub != null ? Bukkit.getWorld(hub.worlds().getFirst()) : null;
        return world != null ? world.getSpawnLocation() : null;
    }

    InventoryStore inventories() {
        return inventories;
    }

    PortalManager portals() {
        return portals;
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
