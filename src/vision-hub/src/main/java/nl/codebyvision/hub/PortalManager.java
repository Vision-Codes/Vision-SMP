package nl.codebyvision.hub;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;

/** Portals are boxes in a world; walking into one sends you to its mode. Stored in portals.yml. */
final class PortalManager {

    record Portal(String name, String mode, String world, BoundingBox box) {
    }

    private final VisionHub plugin;
    private final File file;
    private final Map<String, Portal> portals = new LinkedHashMap<>();

    PortalManager(VisionHub plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "portals.yml");
        load();
    }

    void load() {
        portals.clear();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String name : data.getKeys(false)) {
            ConfigurationSection section = data.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            BoundingBox box = new BoundingBox(
                    section.getDouble("min.x"), section.getDouble("min.y"), section.getDouble("min.z"),
                    section.getDouble("max.x"), section.getDouble("max.y"), section.getDouble("max.z"));
            portals.put(name, new Portal(name, section.getString("mode"), section.getString("world"), box));
        }
    }

    /** Creates a portal covering both corner blocks (inclusive). */
    void create(String name, String mode, Location pos1, Location pos2) {
        BoundingBox box = BoundingBox.of(pos1.getBlock(), pos2.getBlock());
        portals.put(name, new Portal(name, mode, pos1.getWorld().getName(), box));
        save();
    }

    boolean remove(String name) {
        boolean removed = portals.remove(name) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    Map<String, Portal> all() {
        return portals;
    }

    Portal at(Location location) {
        for (Portal portal : portals.values()) {
            if (portal.world().equals(location.getWorld().getName()) && portal.box().contains(location.toVector())) {
                return portal;
            }
        }
        return null;
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Portal portal : portals.values()) {
            ConfigurationSection section = data.createSection(portal.name());
            section.set("mode", portal.mode());
            section.set("world", portal.world());
            section.set("min.x", portal.box().getMinX());
            section.set("min.y", portal.box().getMinY());
            section.set("min.z", portal.box().getMinZ());
            section.set("max.x", portal.box().getMaxX());
            section.set("max.y", portal.box().getMaxY());
            section.set("max.z", portal.box().getMaxZ());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save portals.yml: " + e.getMessage());
        }
    }
}
