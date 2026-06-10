package dev.studio.announcer.spigot.adapter;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SpigotAudienceProvider {

    public Collection<? extends Player> onlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }

    public Optional<Player> findPlayer(String audienceId) {
        if (audienceId == null || audienceId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(audienceId)));
        } catch (IllegalArgumentException ignored) {
            return Optional.ofNullable(Bukkit.getPlayerExact(audienceId));
        }
    }
}
