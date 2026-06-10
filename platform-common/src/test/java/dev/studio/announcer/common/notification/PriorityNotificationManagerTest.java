package dev.studio.announcer.common.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PriorityNotificationManagerTest {
    private static final Instant NOW = Instant.parse("2026-06-10T12:00:00Z");

    @Test
    void firstNotificationDisplaysImmediately() {
        PriorityNotificationManager<String> manager = new PriorityNotificationManager<>();

        PriorityNotificationDecision<String> decision = manager.submit(notification("low", 1, Duration.ofSeconds(5)), NOW);

        assertEquals(PriorityNotificationDecisionType.DISPLAY_NOW, decision.type());
        assertEquals("low", decision.notification().orElseThrow().id());
        assertEquals("low", manager.active("player", NOW).orElseThrow().id());
    }

    @Test
    void higherPriorityNotificationPreemptsActiveAndActiveResumesAfterCompletion() {
        PriorityNotificationManager<String> manager = new PriorityNotificationManager<>();
        PriorityNotification<String> low = notification("low", 1, Duration.ofSeconds(10));
        PriorityNotification<String> high = notification("high", 10, Duration.ofSeconds(2));

        manager.submit(low, NOW);
        PriorityNotificationDecision<String> decision = manager.submit(high, NOW.plusSeconds(3));

        assertEquals(PriorityNotificationDecisionType.DISPLAY_NOW, decision.type());
        assertEquals("high", decision.notification().orElseThrow().id());
        assertEquals("low", decision.preempted().orElseThrow().id());

        Optional<PriorityNotification<String>> resumed = manager.complete("player", "high", NOW.plusSeconds(5));

        assertTrue(resumed.isPresent());
        assertEquals("low", resumed.orElseThrow().id());
        assertEquals(Duration.ofSeconds(7), resumed.orElseThrow().duration());
    }

    @Test
    void lowerPriorityNotificationQueuesUntilActiveCompletes() {
        PriorityNotificationManager<String> manager = new PriorityNotificationManager<>();
        PriorityNotification<String> high = notification("high", 10, Duration.ofSeconds(2));
        PriorityNotification<String> low = notification("low", 1, Duration.ofSeconds(5));

        manager.submit(high, NOW);
        PriorityNotificationDecision<String> decision = manager.submit(low, NOW.plusMillis(100));

        assertEquals(PriorityNotificationDecisionType.QUEUED, decision.type());
        assertEquals("high", manager.active("player", NOW).orElseThrow().id());

        Optional<PriorityNotification<String>> next = manager.complete("player", "high", NOW.plusSeconds(2));

        assertTrue(next.isPresent());
        assertEquals("low", next.orElseThrow().id());
    }

    @Test
    void duplicateNotificationInsideAntiSpamWindowIsSuppressed() {
        PriorityNotificationManager<String> manager = new PriorityNotificationManager<>();
        PriorityNotification<String> notice = new PriorityNotification<>(
                "same",
                "player",
                "payload",
                1,
                Duration.ofSeconds(2),
                Duration.ofSeconds(10));

        manager.submit(notice, NOW);
        PriorityNotificationDecision<String> decision = manager.submit(notice, NOW.plusSeconds(5));

        assertEquals(PriorityNotificationDecisionType.SUPPRESSED, decision.type());
        assertFalse(decision.notification().isPresent());
    }

    @Test
    void clearAudienceRemovesActiveAndQueuedNotifications() {
        PriorityNotificationManager<String> manager = new PriorityNotificationManager<>();
        manager.submit(notification("active", 5, Duration.ofSeconds(10)), NOW);
        manager.submit(notification("queued", 1, Duration.ofSeconds(10)), NOW.plusMillis(50));

        manager.clearAudience("player");

        assertFalse(manager.active("player", NOW).isPresent());
        assertFalse(manager.complete("player", "active", NOW.plusSeconds(1)).isPresent());
    }

    private PriorityNotification<String> notification(String id, int priority, Duration duration) {
        return new PriorityNotification<>(id, "player", id, priority, duration, Duration.ZERO);
    }
}
