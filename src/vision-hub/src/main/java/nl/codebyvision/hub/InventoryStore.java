package nl.codebyvision.hub;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/** Keeps a separate inventory, ender chest, XP, health, hunger, effects and last location per group. */
final class InventoryStore {

    private final VisionHub plugin;
    private final File folder;

    InventoryStore(VisionHub plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "inventories");
    }

    void save(Player player, String group, Location location) {
        YamlConfiguration data = read(player.getUniqueId());
        ConfigurationSection section = data.createSection(group);
        section.set("contents", encode(player.getInventory().getContents()));
        section.set("enderchest", encode(player.getEnderChest().getContents()));
        section.set("level", player.getLevel());
        section.set("exp", player.getExp());
        section.set("health", player.getHealth());
        section.set("food", player.getFoodLevel());
        section.set("saturation", player.getSaturation());
        section.set("effects", new ArrayList<>(player.getActivePotionEffects()));
        section.set("location", location);
        write(player.getUniqueId(), data);
    }

    /** Puts the stored state of the group on the player, or a fresh start when there is none. */
    void load(Player player, String group) {
        ConfigurationSection section = read(player.getUniqueId()).getConfigurationSection(group);

        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        if (section == null) {
            player.getInventory().clear();
            player.getEnderChest().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setHealth(maxHealth(player));
            player.setFoodLevel(20);
            player.setSaturation(5);
            return;
        }

        player.getInventory().setContents(decode(section.getString("contents")));
        player.getEnderChest().setContents(decode(section.getString("enderchest")));
        player.setLevel(section.getInt("level"));
        player.setExp((float) section.getDouble("exp"));
        player.setHealth(Math.min(section.getDouble("health", 20), maxHealth(player)));
        player.setFoodLevel(section.getInt("food", 20));
        player.setSaturation((float) section.getDouble("saturation", 5));
        for (Object effect : section.getList("effects", List.of())) {
            if (effect instanceof PotionEffect potionEffect) {
                player.addPotionEffect(potionEffect);
            }
        }
    }

    Location lastLocation(UUID player, String group) {
        return read(player).getLocation(group + ".location");
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 20;
    }

    private static String encode(ItemStack[] items) {
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(items));
    }

    private static ItemStack[] decode(String data) {
        if (data == null || data.isEmpty()) {
            return new ItemStack[0];
        }
        return ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(data));
    }

    private YamlConfiguration read(UUID player) {
        return YamlConfiguration.loadConfiguration(new File(folder, player + ".yml"));
    }

    private void write(UUID player, YamlConfiguration data) {
        try {
            folder.mkdirs();
            data.save(new File(folder, player + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save inventory of " + player + ": " + e.getMessage());
        }
    }
}
