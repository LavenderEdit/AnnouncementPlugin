package dev.studio.announcer.common.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

public final class PriorityNotificationManager<T> {
    private final Map<String, AudienceState<T>> states = new HashMap<>();
    private long sequence;

    public synchronized PriorityNotificationDecision<T> submit(PriorityNotification<T> notification, Instant now) {
        Instant safeNow = safeNow(now);
        AudienceState<T> state = state(notification.audienceId());
        Optional<PriorityNotification<T>> expiredPromoted = expireActive(state, safeNow);

        Instant lastAccepted = state.lastAccepted.get(notification.id());
        if (lastAccepted != null
                && !notification.antiSpamWindow().isZero()
                && safeNow.isBefore(lastAccepted.plus(notification.antiSpamWindow()))) {
            return new PriorityNotificationDecision<>(
                    PriorityNotificationDecisionType.SUPPRESSED,
                    Optional.empty(),
                    expiredPromoted);
        }
        state.lastAccepted.put(notification.id(), safeNow);

        if (state.active == null) {
            activate(state, notification, safeNow);
            return new PriorityNotificationDecision<>(
                    PriorityNotificationDecisionType.DISPLAY_NOW,
                    Optional.of(notification),
                    expiredPromoted);
        }

        if (expiredPromoted.isPresent()) {
            PriorityNotification<T> promoted = expiredPromoted.get();
            if (notification.priority() > promoted.priority()) {
                PriorityNotification<T> paused = remainingNotification(state.active, safeNow);
                if (paused != null) {
                    enqueue(state, paused);
                }
                activate(state, notification, safeNow);
                return new PriorityNotificationDecision<>(
                        PriorityNotificationDecisionType.DISPLAY_NOW,
                        Optional.of(notification),
                        Optional.ofNullable(paused));
            }
            enqueue(state, notification);
            return new PriorityNotificationDecision<>(
                    PriorityNotificationDecisionType.DISPLAY_NOW,
                    expiredPromoted,
                    Optional.empty());
        }

        PriorityNotification<T> activeNotification = state.active.notification();
        if (notification.priority() > activeNotification.priority()) {
            PriorityNotification<T> paused = remainingNotification(state.active, safeNow);
            if (paused != null) {
                enqueue(state, paused);
            }
            activate(state, notification, safeNow);
            return new PriorityNotificationDecision<>(
                    PriorityNotificationDecisionType.DISPLAY_NOW,
                    Optional.of(notification),
                    Optional.ofNullable(paused));
        }

        enqueue(state, notification);
        return new PriorityNotificationDecision<>(
                PriorityNotificationDecisionType.QUEUED,
                Optional.of(notification),
                Optional.empty());
    }

    public synchronized Optional<PriorityNotification<T>> complete(String audienceId, String notificationId, Instant now) {
        AudienceState<T> state = states.get(audienceId);
        if (state == null || state.active == null || !state.active.notification().id().equals(notificationId)) {
            return Optional.empty();
        }
        state.active = null;
        return promoteNext(state, safeNow(now));
    }

    public synchronized Optional<PriorityNotification<T>> active(String audienceId, Instant now) {
        AudienceState<T> state = states.get(audienceId);
        if (state == null) {
            return Optional.empty();
        }
        expireActive(state, safeNow(now));
        return state.active == null ? Optional.empty() : Optional.of(state.active.notification());
    }

    public synchronized void clearAudience(String audienceId) {
        states.remove(audienceId);
    }

    public synchronized void clearAll() {
        states.clear();
    }

    private AudienceState<T> state(String audienceId) {
        return states.computeIfAbsent(audienceId, ignored -> new AudienceState<>());
    }

    private void activate(AudienceState<T> state, PriorityNotification<T> notification, Instant now) {
        state.active = new ActiveNotification<>(notification, now.plus(notification.duration()));
    }

    private void enqueue(AudienceState<T> state, PriorityNotification<T> notification) {
        state.queue.add(new QueuedNotification<>(notification, sequence++));
    }

    private Optional<PriorityNotification<T>> promoteNext(AudienceState<T> state, Instant now) {
        QueuedNotification<T> next = state.queue.poll();
        if (next == null) {
            return Optional.empty();
        }
        activate(state, next.notification(), now);
        return Optional.of(next.notification());
    }

    private Optional<PriorityNotification<T>> expireActive(AudienceState<T> state, Instant now) {
        if (state.active != null && !now.isBefore(state.active.expiresAt())) {
            state.active = null;
            return promoteNext(state, now);
        }
        return Optional.empty();
    }

    private PriorityNotification<T> remainingNotification(ActiveNotification<T> active, Instant now) {
        Duration remaining = Duration.between(now, active.expiresAt());
        if (remaining.isZero() || remaining.isNegative()) {
            return null;
        }
        return active.notification().withDuration(remaining);
    }

    private Instant safeNow(Instant now) {
        return now == null ? Instant.now() : now;
    }

    private static final class AudienceState<T> {
        private static final Comparator<QueuedNotification<?>> QUEUE_ORDER = Comparator
                .<QueuedNotification<?>, Integer>comparing(entry -> entry.notification().priority())
                .reversed()
                .thenComparingLong(QueuedNotification::sequence);

        private final PriorityQueue<QueuedNotification<T>> queue = new PriorityQueue<>((left, right) ->
                QUEUE_ORDER.compare(left, right));
        private final Map<String, Instant> lastAccepted = new HashMap<>();
        private ActiveNotification<T> active;
    }

    private record ActiveNotification<T>(PriorityNotification<T> notification, Instant expiresAt) {
    }

    private record QueuedNotification<T>(PriorityNotification<T> notification, long sequence) {
    }
}
