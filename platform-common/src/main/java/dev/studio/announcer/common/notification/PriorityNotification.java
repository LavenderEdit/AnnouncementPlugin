package dev.studio.announcer.common.notification;

import java.time.Duration;
import java.util.Objects;

public record PriorityNotification<T>(
        String id,
        String audienceId,
        T payload,
        int priority,
        Duration duration,
        Duration antiSpamWindow) {

    public PriorityNotification {
        id = requireNonBlank(id, "Notification id");
        audienceId = requireNonBlank(audienceId, "Audience id");
        payload = Objects.requireNonNull(payload, "payload");
        if (priority < 0) {
            throw new IllegalArgumentException("Notification priority cannot be negative.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Notification duration must be positive.");
        }
        antiSpamWindow = antiSpamWindow == null ? Duration.ZERO : antiSpamWindow;
        if (antiSpamWindow.isNegative()) {
            throw new IllegalArgumentException("Notification anti-spam window cannot be negative.");
        }
    }

    public PriorityNotification<T> withDuration(Duration newDuration) {
        return new PriorityNotification<>(id, audienceId, payload, priority, newDuration, antiSpamWindow);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }
}
