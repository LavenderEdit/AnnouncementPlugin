package dev.studio.announcer.spigot.listener;

import dev.studio.announcer.api.service.AnnouncementExecutionContext;
import dev.studio.announcer.application.usecase.ExecuteTriggeredAnnouncementsUseCase;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.Map;
import java.util.Objects;

public final class PlayerJoinAnnouncementListener implements Listener {
    private final ExecuteTriggeredAnnouncementsUseCase useCase;

    public PlayerJoinAnnouncementListener(ExecuteTriggeredAnnouncementsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String actorId = event.getPlayer().getUniqueId().toString();
        AnnouncementExecutionContext context = new AnnouncementExecutionContext(actorId, Map.of(
            "player_name", event.getPlayer().getName(),
            "player_uuid", actorId
        ));
        useCase.execute(AnnouncementType.EVENT_JOIN, context);
    }
}
