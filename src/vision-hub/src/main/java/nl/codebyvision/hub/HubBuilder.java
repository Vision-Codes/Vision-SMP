package nl.codebyvision.hub;

import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

/** Builds the hub square around a center point: plaza, fountain, an arch with portal per mode, and the spawn. */
final class HubBuilder {

    private static final String TAG = "visionhub";
    private static final int RADIUS = 24;
    private static final int ARCH_DISTANCE = 18;
    private static final int CLEAR_HEIGHT = 14;

    /** One arch: which mode, which direction (dx, dz) from the center, its blocks and label. */
    private record Arch(String mode, int dx, int dz, Material frame, Material trim, Material path, String label) {
    }

    private static final List<Arch> ARCHES = List.of(
            new Arch("survival", 0, -1, Material.MOSSY_STONE_BRICKS, Material.OAK_LOG, Material.MOSS_BLOCK,
                    "<green><bold>SURVIVAL</bold></green>"),
            new Arch("smp", -1, 0, Material.PURPUR_BLOCK, Material.PURPUR_PILLAR, Material.PURPUR_BLOCK,
                    "<light_purple><bold>SMP</bold></light_purple>"),
            new Arch("parkour", 1, 0, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.PRISMARINE,
                    "<aqua><bold>PARKOUR</bold></aqua>"));

    private final VisionHub plugin;

    HubBuilder(VisionHub plugin) {
        this.plugin = plugin;
    }

    /** Builds with the floor at the block below center. Returns the number of portals created. */
    int build(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1;
        int cz = center.getBlockZ();

        removeLabels(world, center);
        plaza(world, cx, cy, cz);
        fountain(world, cx, cy, cz);

        int portals = 0;
        for (Arch arch : ARCHES) {
            path(world, cx, cy, cz, arch);
            if (plugin.modes().containsKey(arch.mode())) {
                arch(world, cx, cy, cz, arch);
                portals++;
            }
        }
        label(new Location(world, cx + 0.5, cy + 5.5, cz + 0.5),
                "<gold><bold>VISION SMP</bold></gold>\n<gray>Loop door een poort om te spelen");

        // Spawn on the south side, looking north at the fountain and arches
        world.setSpawnLocation(new Location(world, cx + 0.5, cy + 1, cz + 8.5, 180, 0));
        return portals;
    }

    private void plaza(World world, int cx, int cy, int cz) {
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > RADIUS + 0.5) {
                    continue;
                }
                for (int y = 1; y <= CLEAR_HEIGHT; y++) {
                    set(world, cx + x, cy + y, cz + z, Material.AIR);
                }
                set(world, cx + x, cy - 1, cz + z, Material.STONE);

                Material floor;
                if (r > RADIUS - 1.5) {
                    floor = Material.STONE_BRICKS;
                } else if (Math.abs(r - 12) < 0.5) {
                    floor = Material.POLISHED_DEEPSLATE;
                } else if (r < 6) {
                    floor = Material.SMOOTH_QUARTZ;
                } else {
                    floor = (x + z) % 2 == 0 ? Material.POLISHED_ANDESITE : Material.SMOOTH_STONE;
                }
                set(world, cx + x, cy, cz + z, floor);

                // Railing with a lantern post every few blocks
                if (r > RADIUS - 0.5) {
                    boolean post = Math.floorMod(x * 7 + z * 13, 9) == 0;
                    set(world, cx + x, cy + 1, cz + z, post ? Material.STONE_BRICKS : Material.STONE_BRICK_WALL);
                    if (post) {
                        set(world, cx + x, cy + 2, cz + z, Material.LANTERN);
                    }
                }
            }
        }
        // Light in the floor ring
        for (int angle = 0; angle < 360; angle += 30) {
            int x = (int) Math.round(12 * Math.cos(Math.toRadians(angle)));
            int z = (int) Math.round(12 * Math.sin(Math.toRadians(angle)));
            set(world, cx + x, cy, cz + z, Material.SEA_LANTERN);
        }
    }

    private void fountain(World world, int cx, int cy, int cz) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r <= 2.5) {
                    set(world, cx + x, cy, cz + z, Material.PRISMARINE);
                    set(world, cx + x, cy + 1, cz + z, Material.WATER);
                } else if (r <= 3.5) {
                    set(world, cx + x, cy + 1, cz + z, Material.QUARTZ_BRICKS);
                }
            }
        }
        for (int y = 1; y <= 3; y++) {
            set(world, cx, cy + y, cz, Material.QUARTZ_PILLAR);
        }
        set(world, cx, cy + 4, cz, Material.SEA_LANTERN);
    }

    private void path(World world, int cx, int cy, int cz, Arch arch) {
        for (int d = 4; d < ARCH_DISTANCE; d++) {
            for (int s = -1; s <= 1; s++) {
                int x = cx + arch.dx() * d + -arch.dz() * s;
                int z = cz + arch.dz() * d + arch.dx() * s;
                set(world, x, cy, z, s == 0 ? arch.path() : Material.POLISHED_ANDESITE);
            }
        }
    }

    private void arch(World world, int cx, int cy, int cz, Arch arch) {
        int bx = cx + arch.dx() * ARCH_DISTANCE;
        int bz = cz + arch.dz() * ARCH_DISTANCE;
        int sx = -arch.dz();
        int sz = arch.dx();

        for (int s = -3; s <= 3; s++) {
            for (int y = 1; y <= 6; y++) {
                boolean side = Math.abs(s) >= 2;
                boolean top = y >= 5;
                Material material = Material.AIR;
                if (Math.abs(s) == 3 && y <= 5) {
                    material = arch.trim();
                } else if (side || top) {
                    material = arch.frame();
                }
                if (y == 6 && Math.abs(s) == 3) {
                    material = Material.LANTERN;
                }
                set(world, bx + sx * s, cy + y, bz + sz * s, material);
            }
            set(world, bx + sx * s, cy, bz + sz * s, arch.frame());
        }

        // The opening (3 wide, 4 high) is the portal
        Location pos1 = new Location(world, bx - sx, cy + 1, bz - sz);
        Location pos2 = new Location(world, bx + sx, cy + 4, bz + sz);
        plugin.portals().create("hub-" + arch.mode(), arch.mode(), pos1, pos2);

        label(new Location(world, bx + 0.5, cy + 7.3, bz + 0.5), arch.label());
    }

    private static void label(Location location, String text) {
        location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(MiniMessage.miniMessage().deserialize(text));
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(true);
            display.addScoreboardTag(TAG);
        });
    }

    /** Labels from an earlier build, so rebuilding does not stack them. */
    private static void removeLabels(World world, Location center) {
        for (Entity entity : world.getNearbyEntities(center, RADIUS + 5, CLEAR_HEIGHT + 10, RADIUS + 5)) {
            if (entity.getScoreboardTags().contains(TAG)) {
                entity.remove();
            }
        }
    }

    private static void set(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }
}
