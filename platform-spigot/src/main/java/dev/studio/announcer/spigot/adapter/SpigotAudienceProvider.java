package dev.studio.announcer.spigot.adapter;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Proveedor de audiencias y datos de servidor para el entorno Spigot.
 * <p>
 * Nota de diseño: Esta clase no es {@code final} de manera intencional para permitir
 * la creación de subclases y "fakes" durante las pruebas unitarias sin depender de
 * frameworks de simulación pesados como Mockito o MockBukkit.
 */
public class SpigotAudienceProvider {

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

    public String serverName() {
        org.bukkit.Server server = Bukkit.getServer();
        return server != null ? server.getName() : "unknown";
    }

    public int onlinePlayersCount() {
        return onlinePlayers().size();
    }

    public int maxPlayers() {
        org.bukkit.Server server = Bukkit.getServer();
        return server != null ? server.getMaxPlayers() : 100;
    }
}
