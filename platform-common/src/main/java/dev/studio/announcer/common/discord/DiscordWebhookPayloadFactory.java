package dev.studio.announcer.common.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;

public final class DiscordWebhookPayloadFactory {
    private final ObjectMapper objectMapper;

    public DiscordWebhookPayloadFactory() {
        this(new ObjectMapper());
    }

    DiscordWebhookPayloadFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String payload(DiscordOutboundMessage message, DiscordWebhookSettings settings) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            if (!settings.username().isBlank()) {
                root.put("username", settings.username());
            }
            ArrayNode embeds = root.putArray("embeds");
            ObjectNode embed = embeds.addObject();
            embed.put("title", message.title());
            embed.put("description", message.description());
            embed.put("color", message.color() > 0 ? message.color() : settings.color());
            String footer = message.footer().isBlank() ? settings.footer() : message.footer();
            if (!footer.isBlank()) {
                embed.putObject("footer").put("text", footer);
            }
            String thumbnail = message.thumbnailUrl().isBlank() ? settings.thumbnailUrl() : message.thumbnailUrl();
            if (!thumbnail.isBlank()) {
                embed.putObject("thumbnail").put("url", thumbnail);
            }
            if (settings.timestamp() && message.timestamp()) {
                embed.put("timestamp", message.createdAt().toString());
            }
            return objectMapper.writeValueAsString(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not encode Discord webhook payload.", ex);
        }
    }
}
