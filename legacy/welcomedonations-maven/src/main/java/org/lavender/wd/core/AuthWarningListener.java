package org.lavender.wd.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 *
 * @author Studios TKOH!
 */
public class AuthWarningListener implements Listener {

    private final Colorizer colorizer;
    private final String message;

    public AuthWarningListener(Colorizer colorizer, String message) {
        this.colorizer = colorizer;
        this.message = message;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (message == null || message.isEmpty()) {
            return;
        }
        event.getPlayer().sendMessage(colorizer.colorize(message));
    }
}
