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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class PlayerDeathAnnouncementListener implements Listener {
    private final ExecuteTriggeredAnnouncementsUseCase useCase;

    public PlayerDeathAnnouncementListener(ExecuteTriggeredAnnouncementsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        String victimId = victim.getUniqueId().toString();

        Player killer = victim.getKiller();
        String killerName = killer != null ? killer.getName() : "";
        String killerUuid = killer != null ? killer.getUniqueId().toString() : "";

        String deathCause = resolveDeathCause(victim);
        String deathMessage = event.getDeathMessage() != null ? event.getDeathMessage() : "";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("victim_name", victim.getName());
        placeholders.put("victim_uuid", victimId);
        placeholders.put("killer_name", killerName);
        placeholders.put("killer_uuid", killerUuid);
        placeholders.put("death_cause", deathCause);
        placeholders.put("death_message", deathMessage);
        placeholders.put("actor_name", victim.getName());
        placeholders.put("viewer_name", victim.getName());
        placeholders.put("world", victim.getWorld().getName());

        AnnouncementExecutionContext context = new AnnouncementExecutionContext(victimId, placeholders);
        useCase.execute(AnnouncementType.EVENT_DEATH, context);
    }

    private static String resolveDeathCause(Player victim) {
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        if (lastDamage == null) {
            return "unknown";
        }
        EntityDamageEvent.DamageCause cause = lastDamage.getCause();
        return cause != null ? cause.name().toLowerCase().replace('_', ' ') : "unknown";
    }
}
