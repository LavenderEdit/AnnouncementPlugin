package dev.studio.announcer.spigot.listener;

import dev.studio.announcer.spigot.service.PriorityActionBarService;
import dev.studio.announcer.spigot.service.PriorityBossBarService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class NotificationCleanupListener implements Listener {
    private final PriorityActionBarService actionBarService;
    private final PriorityBossBarService bossBarService;

    public NotificationCleanupListener(
            PriorityActionBarService actionBarService,
            PriorityBossBarService bossBarService) {
        this.actionBarService = Objects.requireNonNull(actionBarService, "actionBarService");
        this.bossBarService = Objects.requireNonNull(bossBarService, "bossBarService");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String audienceId = event.getPlayer().getUniqueId().toString();
        actionBarService.clearAudience(audienceId);
        bossBarService.clearAudience(audienceId);
    }
}
