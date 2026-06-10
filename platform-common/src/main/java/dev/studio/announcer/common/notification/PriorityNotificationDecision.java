package dev.studio.announcer.common.notification;

import java.util.Objects;
import java.util.Optional;

public record PriorityNotificationDecision<T>(
        PriorityNotificationDecisionType type,
        Optional<PriorityNotification<T>> notification,
        Optional<PriorityNotification<T>> preempted) {

    public PriorityNotificationDecision {
        type = Objects.requireNonNull(type, "type");
        notification = notification == null ? Optional.empty() : notification;
        preempted = preempted == null ? Optional.empty() : preempted;
    }
}
