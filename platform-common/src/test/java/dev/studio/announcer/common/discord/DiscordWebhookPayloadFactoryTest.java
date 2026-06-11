package dev.studio.announcer.common.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DiscordWebhookPayloadFactoryTest {

    @Test
    void createsEmbedPayloadWithConfiguredDefaults() throws Exception {
        DiscordWebhookPayloadFactory factory = new DiscordWebhookPayloadFactory(new ObjectMapper());
        DiscordWebhookSettings settings = new DiscordWebhookSettings(
                "https://discord.test/webhook",
                "AdvancedAnnouncer",
                0x22AA66,
                "play.example.net",
                "https://example.test/thumb.png",
                true);
        DiscordOutboundMessage message = new DiscordOutboundMessage(
                "Event",
                "<gold>Double XP</gold>",
                0,
                "",
                "",
                true,
                Instant.parse("2026-06-11T10:00:00Z"));

        JsonNode root = new ObjectMapper().readTree(factory.payload(message, settings));

        assertEquals("AdvancedAnnouncer", root.path("username").asText());
        JsonNode embed = root.path("embeds").get(0);
        assertEquals("Event", embed.path("title").asText());
        assertEquals("<gold>Double XP</gold>", embed.path("description").asText());
        assertEquals(0x22AA66, embed.path("color").asInt());
        assertEquals("play.example.net", embed.path("footer").path("text").asText());
        assertEquals("https://example.test/thumb.png", embed.path("thumbnail").path("url").asText());
        assertTrue(embed.has("timestamp"));
    }
}
