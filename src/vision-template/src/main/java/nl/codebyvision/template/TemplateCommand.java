package nl.codebyvision.template;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

final class TemplateCommand implements CommandExecutor, TabCompleter {

    private final VisionTemplate plugin;

    TemplateCommand(VisionTemplate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("template.admin")) {
                sender.sendMessage(plugin.message("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(plugin.message("reloaded"));
            return true;
        }
        sender.sendMessage(plugin.message("info"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("template.admin")) {
            return List.of("reload");
        }
        return List.of();
    }
}
