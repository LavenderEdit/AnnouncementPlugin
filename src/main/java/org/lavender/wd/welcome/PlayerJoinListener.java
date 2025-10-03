package org.lavender.wd.welcome;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 *
 * @author Studios TKOH!
 */
public class PlayerJoinListener implements Listener {

    private final WelcomeService welcomeService;

    public PlayerJoinListener(WelcomeService welcomeService) {
        this.welcomeService = welcomeService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        welcomeService.handleJoin(event.getPlayer());
    }
}
