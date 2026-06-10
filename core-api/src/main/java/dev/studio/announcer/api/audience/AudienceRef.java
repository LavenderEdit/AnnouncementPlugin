package dev.studio.announcer.api.audience;

import java.util.Objects;

public record AudienceRef(AudienceType type, String id) {

    public AudienceRef {
        type = Objects.requireNonNull(type, "type");
        id = id == null ? "" : id.trim();
        if (type == AudienceType.PLAYER && id.isBlank()) {
            throw new IllegalArgumentException("Player audience id cannot be blank.");
        }
    }

    public static AudienceRef player(String id) {
        return new AudienceRef(AudienceType.PLAYER, id);
    }

    public static AudienceRef console() {
        return new AudienceRef(AudienceType.CONSOLE, "console");
    }
}
