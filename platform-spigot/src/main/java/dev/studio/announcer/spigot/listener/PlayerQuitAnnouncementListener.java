package dev.studio.announcer.spigot.listener;

import dev.studio.announcer.api.service.AnnouncementExecutionContext;
import dev.studio.announcer.application.usecase.ExecuteTriggeredAnnouncementsUseCase;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitAnnouncementListener implements Listener {
    private final ExecuteTriggeredAnnouncementsUseCase useCase;

    public PlayerQuitAnnouncementListener(ExecuteTriggeredAnnouncementsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String actorId = player.getUniqueId().toString();
        String actorWorld = player.getWorld().getName();
        String onlinePlayers = String.valueOf(Bukkit.getOnlinePlayers().size());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("actor_name", player.getName());
        placeholders.put("actor_uuid", actorId);
        placeholders.put("actor_world", actorWorld);
        placeholders.put("viewer_name", player.getName());
        placeholders.put("online_players", onlinePlayers);

        AnnouncementExecutionContext context = new AnnouncementExecutionContext(actorId, placeholders);
        useCase.execute(AnnouncementType.EVENT_QUIT, context);
    }
}
