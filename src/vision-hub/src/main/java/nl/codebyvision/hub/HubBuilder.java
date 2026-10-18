package nl.codebyvision.hub;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

/**
 * Builds the hub as a spooky village: a plaza with a fountain, and a Victorian house per mode whose front door
 * is the portal. Everything is generated around a center point, so it can be rebuilt anywhere.
 */
final class HubBuilder {

    private static final String TAG = "visionhub";
    private static final int RADIUS = 42;
    private static final int CLEAR_HEIGHT = 48;
    private static final int PLAZA = 9;
    private static final int HOUSE_DISTANCE = 22;
    private static final int FLOOR = 5;
    private static final int HALF = 6;
    private static final int DEPTH = 13;

    private record Tower(int u, int v, int extra) {
    }

    /** One house: the mode it leads to, the direction (dx, dz) from the center, and its look. */
    private record House(String mode, int dx, int dz, String label, Material wall, Material log, Material beam,
                         Material accent, Material light, Material roof, Material roofStairs, Material fence,
                         int floors, boolean porch, List<Tower> towers) {
    }

    private static final List<House> HOUSES = List.of(
            // Haunted House: white walls, dark spires, green light, porch, tall middle tower
            new House("survival", 0, -1, "<green><bold>SURVIVAL</bold></green>", Material.CALCITE,
                    Material.DARK_OAK_LOG, Material.DARK_OAK_PLANKS, Material.DARK_PRISMARINE, Material.VERDANT_FROGLIGHT,
                    Material.DEEPSLATE_TILES, Material.DEEPSLATE_TILE_STAIRS, Material.DARK_OAK_FENCE, 3, true,
                    List.of(new Tower(-HALF, 0, 4), new Tower(HALF, 0, 4), new Tower(0, DEPTH / 2, 11))),
            // Monster House: teal walls, brown beams, grey roof, yellow windows with red frames, porch
            new House("smp", -1, 0, "<light_purple><bold>SMP</bold></light_purple>", Material.WAXED_OXIDIZED_CUT_COPPER,
                    Material.SPRUCE_LOG, Material.SPRUCE_PLANKS, Material.CRIMSON_PLANKS, Material.OCHRE_FROGLIGHT,
                    Material.STONE_BRICKS, Material.STONE_BRICK_STAIRS, Material.SPRUCE_FENCE, 2, true,
                    List.of(new Tower(-HALF, 0, 5), new Tower(HALF, 0, 5))),
            // Manor: mud bricks, dark wood, purple light, one side tower
            new House("parkour", 1, 0, "<aqua><bold>PARKOUR</bold></aqua>", Material.MUD_BRICKS,
                    Material.DARK_OAK_LOG, Material.DARK_OAK_PLANKS, Material.PURPUR_BLOCK, Material.PEARLESCENT_FROGLIGHT,
                    Material.DEEPSLATE_TILES, Material.DEEPSLATE_TILE_STAIRS, Material.DARK_OAK_FENCE, 2, true,
                    List.of(new Tower(HALF, DEPTH / 2, 7))));

    private final VisionHub plugin;
    private final List<Block> fences = new ArrayList<>();
    private World world;
    private int cx;
    private int cy;
    private int cz;

    HubBuilder(VisionHub plugin) {
        this.plugin = plugin;
    }

    /** Builds with the ground at the block below center. Returns the number of portals created. */
    int build(Location center) {
        world = center.getWorld();
        cx = center.getBlockX();
        cy = center.getBlockY() - 1;
        cz = center.getBlockZ();

        removeLabels(center);
        ground();
        plaza();

        int portals = 0;
        for (House house : HOUSES) {
            Frame frame = new Frame(cx + house.dx() * HOUSE_DISTANCE, cy, cz + house.dz() * HOUSE_DISTANCE,
                    house.dx(), house.dz());
            path(house);
            house(frame, house);
            if (plugin.modes().containsKey(house.mode())) {
                plugin.portals().create("hub-" + house.mode(), house.mode(), frame.loc(-1, 2, 0), frame.loc(1, 5, 1));
                portals++;
            }
            label(frame.loc(0, 8, -1).add(0.5, 0.5, 0.5),
                    house.label() + "\n<gray>Loop door de deur");
        }

        trees();
        graveyard();
        pumpkins();
        perimeter();
        connectFences();

        label(new Location(world, cx + 0.5, cy + 7.5, cz + 0.5),
                "<gold><bold>VISION SMP</bold></gold>\n<gray>Kies een huis en loop naar binnen");
        world.setSpawnLocation(new Location(world, cx + 0.5, cy + 1, cz + 15.5, 180, 0));
        return portals;
    }

    // ---- terrain ----

    private void ground() {
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                if (x * x + z * z > (RADIUS + 0.5) * (RADIUS + 0.5)) {
                    continue;
                }
                for (int y = 1; y <= CLEAR_HEIGHT; y++) {
                    set(cx + x, cy + y, cz + z, Material.AIR);
                }
                set(cx + x, cy - 2, cz + z, Material.STONE);
                set(cx + x, cy - 1, cz + z, Material.DIRT);
                int patch = hash(Math.floorDiv(x, 5), Math.floorDiv(z, 5)) % 10;
                int speck = hash(x, z) % 10;
                Material top = Material.GRASS_BLOCK;
                if (patch < 2) {
                    top = speck < 6 ? Material.PODZOL : Material.COARSE_DIRT;
                } else if (patch < 3) {
                    top = speck < 5 ? Material.COARSE_DIRT : Material.ROOTED_DIRT;
                } else if (speck == 0) {
                    top = Material.COARSE_DIRT;
                }
                set(cx + x, cy, cz + z, top);
                if (top == Material.GRASS_BLOCK && hash(z, x) % 9 == 0) {
                    set(cx + x, cy + 1, cz + z, Material.SHORT_DRY_GRASS);
                }
            }
        }
    }

    private void plaza() {
        for (int x = -PLAZA; x <= PLAZA; x++) {
            for (int z = -PLAZA; z <= PLAZA; z++) {
                if (x * x + z * z > PLAZA * PLAZA) {
                    continue;
                }
                int h = hash(x, z) % 10;
                Material stone = h < 4 ? Material.COBBLESTONE : h < 7 ? Material.MOSSY_COBBLESTONE
                        : h < 9 ? Material.STONE_BRICKS : Material.CRACKED_STONE_BRICKS;
                set(cx + x, cy, cz + z, stone);
                set(cx + x, cy + 1, cz + z, Material.AIR);
            }
        }
        // Fountain
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r <= 2.5) {
                    set(cx + x, cy, cz + z, Material.BLACKSTONE);
                    set(cx + x, cy + 1, cz + z, Material.WATER);
                } else if (r <= 3.5) {
                    set(cx + x, cy + 1, cz + z, Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
        }
        for (int y = 1; y <= 4; y++) {
            set(cx, cy + y, cz, y == 4 ? Material.CHISELED_POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS);
        }
        set(cx, cy + 5, cz, Material.SOUL_LANTERN);
        for (int[] corner : new int[][] {{6, 6}, {-6, 6}, {6, -6}, {-6, -6}}) {
            lampPost(cx + corner[0], cz + corner[1], Material.DARK_OAK_FENCE);
        }
    }

    private void path(House house) {
        int sx = -house.dz();
        int sz = house.dx();
        int end = HOUSE_DISTANCE - (house.porch() ? 6 : 2);
        for (int d = PLAZA - 1; d <= end; d++) {
            for (int s = -1; s <= 1; s++) {
                int x = cx + house.dx() * d + sx * s;
                int z = cz + house.dz() * d + sz * s;
                Material material = s == 0 ? Material.DIRT_PATH
                        : hash(x, z) % 3 == 0 ? Material.MOSSY_COBBLESTONE : Material.COBBLESTONE;
                set(x, cy, z, material);
                set(x, cy + 1, z, Material.AIR);
            }
            if (d % 5 == 0 && d > PLAZA + 1) {
                lampPost(cx + house.dx() * d + sx * 3, cz + house.dz() * d + sz * 3, house.fence());
                lampPost(cx + house.dx() * d - sx * 3, cz + house.dz() * d - sz * 3, house.fence());
            }
        }
    }

    // ---- houses ----

    private void house(Frame f, House h) {
        int top = 1 + h.floors() * FLOOR;

        // Foundation and floor
        for (int u = -HALF; u <= HALF; u++) {
            for (int v = 0; v < DEPTH; v++) {
                f.set(u, 0, v, Material.COBBLED_DEEPSLATE);
                f.set(u, 1, v, Material.POLISHED_DEEPSLATE);
            }
        }

        // Walls with timber posts every 4 blocks, a beam on top of every floor and windows in between
        for (int floor = 0; floor < h.floors(); floor++) {
            int base = 2 + floor * FLOOR;
            for (int y = base; y < base + FLOOR; y++) {
                int row = y - base;
                for (int u = -HALF; u <= HALF; u++) {
                    for (int v = 0; v < DEPTH; v++) {
                        boolean front = v == 0 || v == DEPTH - 1;
                        boolean side = u == -HALF || u == HALF;
                        if (!front && !side) {
                            continue;
                        }
                        int along = front ? u + HALF : v;
                        Material m = h.wall();
                        if (along % 4 == 0) {
                            m = h.log();
                        } else if (row == FLOOR - 1) {
                            m = h.beam();
                        } else if (along % 4 == 2) {
                            m = row == 0 || row == 3 ? h.accent() : h.light();
                        }
                        f.set(u, y, v, m);
                    }
                }
            }
        }

        roof(f, h, top);
        if (h.porch()) {
            porch(f, h);
        }
        for (Tower tower : h.towers()) {
            tower(f, h, tower, top + tower.extra());
        }

        // Front door (the portal) with a timber frame and steps
        for (int y = 2; y <= 6; y++) {
            for (int u = -2; u <= 2; u++) {
                boolean frame = Math.abs(u) == 2 || y == 6;
                f.set(u, y, 0, frame ? h.beam() : Material.AIR);
                if (!frame) {
                    f.set(u, y, 1, Material.AIR);
                }
            }
        }
        for (int u = -1; u <= 1; u++) {
            f.stairs(u, 1, h.porch() ? -5 : -1, Material.STONE_BRICK_STAIRS, 0, 1);
        }
        f.set(-3, 3, -1, Material.LANTERN);
        f.set(3, 3, -1, Material.LANTERN);
        f.set(-3, 2, -1, h.fence());
        f.set(3, 2, -1, h.fence());
    }

    /** Steep gable roof with the ridge running front to back, overhanging every side by one block. */
    private void roof(Frame f, House h, int top) {
        int layer = 0;
        for (int edge = HALF + 1; edge > 0; edge--, layer++) {
            int y = top + 1 + layer;
            for (int v = -1; v <= DEPTH; v++) {
                f.stairs(-edge, y, v, h.roofStairs(), 1, 0);
                f.stairs(edge, y, v, h.roofStairs(), -1, 0);
            }
            // Gable ends
            for (int u = -edge + 1; u <= edge - 1; u++) {
                Material m = u == 0 && (layer == 1 || layer == 2) ? h.light() : h.wall();
                f.set(u, y, 0, m);
                f.set(u, y, DEPTH - 1, m);
            }
        }
        int ridge = top + 1 + layer;
        for (int v = -1; v <= DEPTH; v++) {
            f.set(0, ridge, v, h.roof());
            if (v % 3 == 0) {
                f.set(0, ridge + 1, v, Material.LIGHTNING_ROD);
            }
        }
    }

    private void porch(Frame f, House h) {
        for (int u = -HALF; u <= HALF; u++) {
            for (int v = -4; v <= -1; v++) {
                f.set(u, 1, v, Material.SPRUCE_PLANKS);
                f.set(u, 6, v, h.roof());
            }
            f.stairs(u, 6, -5, h.roofStairs(), 0, 1);
            if ((u + HALF) % 4 == 0) {
                for (int y = 2; y <= 5; y++) {
                    f.set(u, y, -4, h.log());
                }
            } else if (Math.abs(u) > 1) {
                fence(f.block(u, 2, -4), h.fence());
            }
        }
    }

    /** Square tower (5x5) with windows on every side and a pointed spire. */
    private void tower(Frame f, House h, Tower t, int top) {
        for (int y = 1; y <= top; y++) {
            for (int du = -2; du <= 2; du++) {
                for (int dv = -2; dv <= 2; dv++) {
                    boolean edge = Math.abs(du) == 2 || Math.abs(dv) == 2;
                    if (y <= 1) {
                        f.set(t.u() + du, y, t.v() + dv, Material.POLISHED_DEEPSLATE);
                        continue;
                    }
                    if (!edge) {
                        continue;
                    }
                    boolean corner = Math.abs(du) == 2 && Math.abs(dv) == 2;
                    boolean middle = du == 0 || dv == 0;
                    int row = (y - 2) % FLOOR;
                    Material m = h.wall();
                    if (corner) {
                        m = h.log();
                    } else if (row == FLOOR - 1) {
                        m = h.beam();
                    } else if (middle && (row == 1 || row == 2)) {
                        m = h.light();
                    }
                    f.set(t.u() + du, y, t.v() + dv, m);
                }
            }
        }
        // Spire: rings that shrink each layer, stairs on the sides and blocks on the corners
        int y = top + 1;
        for (int r = 3; r >= 1; r--, y++) {
            for (int du = -r; du <= r; du++) {
                for (int dv = -r; dv <= r; dv++) {
                    if (Math.abs(du) != r && Math.abs(dv) != r) {
                        continue;
                    }
                    if (Math.abs(du) == r && Math.abs(dv) == r) {
                        f.set(t.u() + du, y, t.v() + dv, h.roof());
                    } else if (Math.abs(du) == r) {
                        f.stairs(t.u() + du, y, t.v() + dv, h.roofStairs(), -Integer.signum(du), 0);
                    } else {
                        f.stairs(t.u() + du, y, t.v() + dv, h.roofStairs(), 0, -Integer.signum(dv));
                    }
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            f.set(t.u(), y + i, t.v(), h.roof());
        }
        f.set(t.u(), y + 3, t.v(), Material.LIGHTNING_ROD);
    }

    // ---- decoration ----

    private void trees() {
        int[][] spots = {{-14, -12}, {14, -13}, {-30, -18}, {30, -20}, {-17, 14}, {18, 13}, {-33, 8}, {34, 9},
                {-8, -33}, {9, -34}, {-26, 28}, {27, 27}};
        for (int[] spot : spots) {
            deadTree(cx + spot[0], cz + spot[1]);
        }
    }

    private void deadTree(int x, int z) {
        int seed = hash(x, z);
        int height = 5 + seed % 3;
        for (int y = 1; y <= height; y++) {
            set(x, cy + y, z, Material.MANGROVE_LOG);
        }
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}};
        int branches = 3 + seed % 2;
        for (int b = 0; b < branches; b++) {
            int[] d = directions[(seed / 7 + b * 2) % directions.length];
            int bx = x;
            int bz = z;
            int by = cy + height - 2 + b % 3;
            for (int step = 0; step < 4; step++) {
                bx += d[0];
                bz += d[1];
                if (step % 2 == 1) {
                    by++;
                }
                set(bx, by, bz, step == 3 ? Material.MANGROVE_ROOTS : Material.MANGROVE_LOG);
            }
        }
        set(x + 1, cy, z, Material.ROOTED_DIRT);
        set(x - 1, cy, z + 1, Material.PODZOL);
    }

    private void graveyard() {
        for (int side : new int[] {-1, 1}) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 4; col++) {
                    int x = cx + side * (16 + col * 3);
                    int z = cz + 20 + row * 4;
                    int h = hash(x, z) % 4;
                    set(x, cy, z, Material.PODZOL);
                    set(x, cy, z + 1, Material.COARSE_DIRT);
                    set(x, cy, z + 2, Material.PODZOL);
                    set(x, cy + 1, z, h == 0 ? Material.STONE_BRICK_WALL : Material.MOSSY_STONE_BRICK_WALL);
                    if (h == 1) {
                        set(x, cy + 2, z, Material.MOSSY_STONE_BRICK_WALL);
                    }
                }
            }
        }
    }

    private void pumpkins() {
        int[][] spots = {{-4, 11}, {4, 12}, {-11, -4}, {11, 5}, {-5, -11}, {6, -11}, {-20, 20}, {21, 22},
                {-12, 30}, {12, 31}, {-36, -2}, {37, 2}, {-2, -38}};
        for (int i = 0; i < spots.length; i++) {
            int x = cx + spots[i][0];
            int z = cz + spots[i][1];
            Material pumpkin = i % 3 == 0 ? Material.CARVED_PUMPKIN : Material.JACK_O_LANTERN;
            BlockFace face = Math.abs(spots[i][0]) > Math.abs(spots[i][1])
                    ? (spots[i][0] > 0 ? BlockFace.WEST : BlockFace.EAST)
                    : (spots[i][1] > 0 ? BlockFace.NORTH : BlockFace.SOUTH);
            directional(world.getBlockAt(x, cy + 1, z), pumpkin, face);
        }
    }

    /** Low fence around the village with stone posts and lanterns. */
    private void perimeter() {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r < RADIUS - 0.5 || r > RADIUS + 0.5) {
                    continue;
                }
                if (Math.floorMod(x * 31 + z * 17, 11) == 0) {
                    set(cx + x, cy + 1, cz + z, Material.STONE_BRICKS);
                    set(cx + x, cy + 2, cz + z, Material.LANTERN);
                } else {
                    fence(world.getBlockAt(cx + x, cy + 1, cz + z), Material.DARK_OAK_FENCE);
                }
            }
        }
    }

    private void lampPost(int x, int z, Material fence) {
        for (int y = 1; y <= 3; y++) {
            set(x, cy + y, z, fence);
        }
        set(x, cy + 4, z, Material.LANTERN);
    }

    // ---- helpers ----

    private void label(Location location, String text) {
        world.spawn(location, TextDisplay.class, display -> {
            display.text(MiniMessage.miniMessage().deserialize(text));
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(true);
            display.addScoreboardTag(TAG);
        });
    }

    /** Labels from an earlier build, so rebuilding does not stack them. */
    private void removeLabels(Location center) {
        for (Entity entity : world.getNearbyEntities(center, RADIUS + 5, CLEAR_HEIGHT + 10, RADIUS + 5)) {
            if (entity.getScoreboardTags().contains(TAG)) {
                entity.remove();
            }
        }
    }

    private void fence(Block block, Material material) {
        block.setType(material, false);
        fences.add(block);
    }

    /** Blocks are placed without physics, so fences are joined to their neighbours afterwards. */
    private void connectFences() {
        BlockFace[] sides = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (Block block : fences) {
            if (!(block.getBlockData() instanceof MultipleFacing data)) {
                continue;
            }
            for (BlockFace side : sides) {
                Block next = block.getRelative(side);
                data.setFace(side, next.getBlockData() instanceof MultipleFacing || next.getType().isOccluding());
            }
            block.setBlockData(data, false);
        }
        fences.clear();
    }

    private void set(int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (material == Material.AIR && block.getType().isAir()) {
            return;
        }
        block.setType(material, false);
    }

    private static void directional(Block block, Material material, BlockFace face) {
        BlockData data = material.createBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(face);
        }
        block.setBlockData(data, false);
    }

    private static BlockFace face(int dx, int dz) {
        if (dx > 0) {
            return BlockFace.EAST;
        }
        if (dx < 0) {
            return BlockFace.WEST;
        }
        return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    /** Same pseudo-random number for the same spot, so a rebuild looks identical. */
    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return Math.floorMod(h, 1000);
    }

    /**
     * Local coordinates for a house: u runs sideways, v runs from the front door (v = 0, facing the plaza)
     * to the back, y is up from the ground.
     */
    private final class Frame {
        private final int ox;
        private final int oy;
        private final int oz;
        private final int fx;
        private final int fz;
        private final int sx;
        private final int sz;

        Frame(int ox, int oy, int oz, int fx, int fz) {
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.fx = fx;
            this.fz = fz;
            this.sx = -fz;
            this.sz = fx;
        }

        Block block(int u, int y, int v) {
            return world.getBlockAt(ox + sx * u + fx * v, oy + y, oz + sz * u + fz * v);
        }

        Location loc(int u, int y, int v) {
            return block(u, y, v).getLocation();
        }

        void set(int u, int y, int v, Material material) {
            Block block = block(u, y, v);
            if (material == Material.AIR && block.getType().isAir()) {
                return;
            }
            block.setType(material, false);
        }

        /** Stairs rising towards the local direction (du, dv). */
        void stairs(int u, int y, int v, Material material, int du, int dv) {
            directional(block(u, y, v), material, face(sx * du + fx * dv, sz * du + fz * dv));
        }
    }
}
