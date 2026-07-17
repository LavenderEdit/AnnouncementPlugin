package dev.studio.announcer.application.usecase;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.util.Objects;
import java.util.Set;

public final class TriggerMatcher {
    private final String localServerId;
    private final Set<String> localGroups;

    public TriggerMatcher(String localServerId, Set<String> localGroups) {
        this.localServerId = localServerId == null ? "" : localServerId.trim();
        this.localGroups = localGroups == null ? Set.of() : Set.copyOf(localGroups);
    }

    public boolean matches(Announcement announcement, AnnouncementType type) {
        if (announcement == null || !announcement.enabled()) {
            return false;
        }
        if (announcement.type() != type) {
            return false;
        }
        if (!announcement.targetServers().isEmpty() && !localServerId.isBlank()
                && announcement.targetServers().stream().noneMatch(server -> server.equalsIgnoreCase(localServerId))) {
            return false;
        }
        if (!announcement.targetGroups().isEmpty() && !localGroups.isEmpty()
                && announcement.targetGroups().stream().noneMatch(group -> localGroups.stream().anyMatch(lg -> lg.equalsIgnoreCase(group)))) {
            return false;
        }
        return true;
    }
}
