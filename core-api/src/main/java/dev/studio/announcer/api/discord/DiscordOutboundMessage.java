package dev.studio.announcer.api.discord;

import java.time.Instant;

public record DiscordOutboundMessage(
        String title,
        String description,
        int color,
        String footer,
        String thumbnailUrl,
        boolean timestamp,
        Instant createdAt) {

    public DiscordOutboundMessage {
        title = normalize(title);
        description = normalize(description);
        footer = normalize(footer);
        thumbnailUrl = normalize(thumbnailUrl);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public DiscordOutboundMessage(String title, String description) {
        this(title, description, 0xF6C344, "", "", true, null);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
