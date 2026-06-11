package dev.studio.announcer.api.discord;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record DiscordInboundMessage(
        String channelId,
        String userId,
        String username,
        String content,
        Set<String> roleIds,
        Instant createdAt) {

    public DiscordInboundMessage {
        channelId = normalize(channelId);
        userId = normalize(userId);
        username = normalize(username);
        content = normalize(content);
        roleIds = roleIds == null ? Set.of() : roleIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
