package dev.studio.announcer.common.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InternalPlaceholderResolverTest {

    @Test
    void resolvesInternalPlaceholdersWithoutMinecraftServer() {
        InternalPlaceholderResolver resolver = new InternalPlaceholderResolver();
        PlaceholderContext context = PlaceholderContext.of(Map.of(
                "player_name", "Nyx",
                "server_name", "lobby-01",
                "online_players", "42"));

        String resolved = resolver.resolve(
                "%player_name% on %server_name% sees %online_players%%newline%players",
                context);

        assertEquals("Nyx on lobby-01 sees 42" + System.lineSeparator() + "players", resolved);
    }

    @Test
    void resolvesAdvancedInternalPlaceholdersWithFallbacks() {
        InternalPlaceholderResolver resolver = new InternalPlaceholderResolver(
                Clock.fixed(Instant.parse("2026-06-11T15:30:45Z"), ZoneOffset.UTC));
        PlaceholderContext context = PlaceholderContext.of(Map.of(
                "player_name", "Nyx",
                "server_group", "survival"));

        String resolved = resolver.resolve("%player_displayname% %server_group% %time% %date%", context);

        assertEquals("Nyx survival 15:30:45 2026-06-11", resolved);
    }
}
