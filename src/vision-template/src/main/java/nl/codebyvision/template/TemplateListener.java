package nl.codebyvision.template;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class TemplateListener implements Listener {

    private final VisionTemplate plugin;

    TemplateListener(VisionTemplate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("welcome-on-join")) {
            event.getPlayer().sendMessage(plugin.message("welcome"));
        }
    }
}
