package dev.studio.announcer.domain.announcement.option;

import java.time.Duration;
import java.util.Optional;

public record ActionBarOptions(
        String message,
        Duration duration,
        int priority,
        String permission,
        Duration antiSpamWindow) {

    public ActionBarOptions {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("ActionBar message cannot be blank.");
        }
        duration = positiveDuration(duration, "ActionBar duration");
        if (priority < 0) {
            throw new IllegalArgumentException("ActionBar priority cannot be negative.");
        }
        antiSpamWindow = antiSpamWindow == null ? Duration.ZERO : antiSpamWindow;
        if (antiSpamWindow.isNegative()) {
            throw new IllegalArgumentException("ActionBar anti-spam window cannot be negative.");
        }
        permission = normalize(permission);
    }

    public Optional<String> permissionValue() {
        return Optional.ofNullable(permission);
    }

    private static Duration positiveDuration(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
