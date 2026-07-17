package dev.studio.announcer.api.service;

import java.util.Map;
import java.util.Objects;

public record AnnouncementExecutionContext(String actorId, Map<String, String> eventPlaceholders) {
    public AnnouncementExecutionContext {
        actorId = Objects.requireNonNull(actorId, "actorId");
        eventPlaceholders = eventPlaceholders == null ? Map.of() : Map.copyOf(eventPlaceholders);
    }
}
