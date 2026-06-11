package dev.studio.announcer.spigot.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscordSrvBridgeServiceTest {

    @Test
    void acceptsOnlyWhitelistedChannelsAndRoles() {
        DiscordSrvBridgeService service = new DiscordSrvBridgeService(new DiscordSrvSettings(
                true,
                Set.of("announcements"),
                Set.of("staff"),
                Duration.ofSeconds(3),
                "<aqua>%discord_user%</aqua>: %discord_message%"));
        List<DiscordInboundMessage> accepted = new ArrayList<>();
        service.setInboundHandler(accepted::add);

        service.acceptInbound(new DiscordInboundMessage("general", "u1", "Lavender", "ignored", Set.of("staff"), Instant.now()));
        service.acceptInbound(new DiscordInboundMessage("announcements", "u2", "NoRole", "ignored", Set.of("member"), Instant.now()));
        service.acceptInbound(new DiscordInboundMessage("announcements", "u3", "Admin", "hello", Set.of("staff"), Instant.now()));

        assertEquals(List.of("hello"), accepted.stream().map(DiscordInboundMessage::content).toList());
    }

    @Test
    void appliesPerUserCooldown() {
        DiscordSrvBridgeService service = new DiscordSrvBridgeService(new DiscordSrvSettings(
                true,
                Set.of(),
                Set.of(),
                Duration.ofSeconds(3),
                "%discord_message%"));
        List<DiscordInboundMessage> accepted = new ArrayList<>();
        service.setInboundHandler(accepted::add);
        Instant now = Instant.parse("2026-06-11T10:00:00Z");

        service.acceptInbound(new DiscordInboundMessage("announcements", "u1", "Admin", "first", Set.of(), now));
        service.acceptInbound(new DiscordInboundMessage("announcements", "u1", "Admin", "second", Set.of(), now.plusSeconds(1)));
        service.acceptInbound(new DiscordInboundMessage("announcements", "u1", "Admin", "third", Set.of(), now.plusSeconds(4)));

        assertEquals(List.of("first", "third"), accepted.stream().map(DiscordInboundMessage::content).toList());
    }
}
