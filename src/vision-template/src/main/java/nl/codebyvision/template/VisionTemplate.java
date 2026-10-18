package nl.codebyvision.template;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class VisionTemplate extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        TemplateCommand command = new TemplateCommand(this);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("template"), "command missing in plugin.yml");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new TemplateListener(this), this);
    }

    /** Message from config.yml (messages.<key>) with the prefix in front, in MiniMessage format. */
    public Component message(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String text = getConfig().getString("messages." + key, key);
        return MiniMessage.miniMessage().deserialize(prefix + text);
    }
}
