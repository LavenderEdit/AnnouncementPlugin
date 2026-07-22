package dev.studio.announcer.spigot.listener;

import dev.studio.announcer.api.service.AnnouncementExecutionContext;
import dev.studio.announcer.application.usecase.ExecuteTriggeredAnnouncementsUseCase;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public final class PlayerWorldChangeAnnouncementListener implements Listener {
    private final ExecuteTriggeredAnnouncementsUseCase useCase;

    public PlayerWorldChangeAnnouncementListener(ExecuteTriggeredAnnouncementsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String actorId = player.getUniqueId().toString();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();
        String onlinePlayers = String.valueOf(Bukkit.getOnlinePlayers().size());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("actor_name", player.getName());
        placeholders.put("player_name", player.getName());
        placeholders.put("player_uuid", actorId);
        placeholders.put("from_world", fromWorld);
        placeholders.put("to_world", toWorld);
        placeholders.put("world", toWorld);
        placeholders.put("viewer_name", player.getName());
        placeholders.put("online_players", onlinePlayers);

        AnnouncementExecutionContext context = new AnnouncementExecutionContext(actorId, placeholders);
        useCase.execute(AnnouncementType.EVENT_WORLD_CHANGE, context);
    }
}
