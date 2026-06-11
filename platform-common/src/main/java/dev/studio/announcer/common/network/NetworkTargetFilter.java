package dev.studio.announcer.common.network;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class NetworkTargetFilter {
    private final String localServerId;
    private final Set<String> localGroups;
    private final boolean ignoreSelfOrigin;

    public NetworkTargetFilter(String localServerId, Set<String> localGroups, boolean ignoreSelfOrigin) {
        this.localServerId = normalize(localServerId);
        this.localGroups = normalizeSet(localGroups);
        this.ignoreSelfOrigin = ignoreSelfOrigin;
    }

    public boolean accepts(NetworkBroadcastRequest request) {
        if (request == null) {
            return false;
        }
        if (ignoreSelfOrigin && !localServerId.isBlank() && localServerId.equalsIgnoreCase(request.originServer())) {
            return false;
        }
        if (!request.targetServers().isEmpty() && request.targetServers().stream().noneMatch(this::isLocalServer)) {
            return false;
        }
        Set<String> requestedGroups = request.targetGroups();
        if (!requestedGroups.isEmpty() && requestedGroups.stream().noneMatch(this::isLocalGroup)) {
            return false;
        }
        return true;
    }

    private boolean isLocalServer(String value) {
        return localServerId.equalsIgnoreCase(value);
    }

    private boolean isLocalGroup(String value) {
        return localGroups.stream().anyMatch(group -> group.equalsIgnoreCase(value));
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
