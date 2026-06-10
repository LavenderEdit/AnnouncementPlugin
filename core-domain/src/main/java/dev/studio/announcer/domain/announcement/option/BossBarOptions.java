package dev.studio.announcer.domain.announcement.option;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record BossBarOptions(
        String title,
        BossBarColor color,
        BossBarOverlay overlay,
        float progress,
        Duration duration,
        int priority,
        String permission,
        boolean animatedProgress,
        boolean autoHide) {

    public BossBarOptions {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("BossBar title cannot be blank.");
        }
        color = Objects.requireNonNull(color, "color");
        overlay = Objects.requireNonNull(overlay, "overlay");
        if (progress < 0.0f || progress > 1.0f) {
            throw new IllegalArgumentException("BossBar progress must be between 0.0 and 1.0.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("BossBar duration must be positive.");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("BossBar priority cannot be negative.");
        }
        permission = normalize(permission);
    }

    public Optional<String> permissionValue() {
        return Optional.ofNullable(permission);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
