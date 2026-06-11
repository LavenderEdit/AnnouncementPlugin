package dev.studio.announcer.api.network;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record NetworkBroadcastRequest(
        String messageId,
        String originServer,
        Set<String> targetServers,
        String targetGroup,
        Set<String> targetGroups,
        String permission,
        String content,
        String packetType,
        String sound,
        Instant createdAt) {

    public NetworkBroadcastRequest {
        messageId = normalize(messageId);
        if (messageId.isBlank()) {
            messageId = UUID.randomUUID().toString();
        }
        originServer = normalize(originServer);
        targetServers = normalizeSet(targetServers);
        targetGroup = normalize(targetGroup);
        targetGroups = normalizeSet(targetGroups);
        if (!targetGroup.isBlank() && !targetGroups.contains(targetGroup)) {
            targetGroups = java.util.stream.Stream.concat(targetGroups.stream(), java.util.stream.Stream.of(targetGroup))
                    .collect(Collectors.toUnmodifiableSet());
        }
        permission = normalize(permission);
        content = normalize(content);
        packetType = normalize(packetType);
        if (packetType.isBlank()) {
            packetType = "CHAT";
        }
        sound = normalize(sound);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public NetworkBroadcastRequest(
            String originServer,
            String targetGroup,
            String permission,
            String content,
            String packetType,
            String sound) {
        this(
                "",
                originServer,
                Set.of(),
                targetGroup,
                targetGroup == null || targetGroup.isBlank() ? Set.of() : Set.of(targetGroup),
                permission,
                content,
                packetType,
                sound,
                null);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Set<String> normalizeSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
