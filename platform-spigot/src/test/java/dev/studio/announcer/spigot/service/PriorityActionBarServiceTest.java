package dev.studio.announcer.spigot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class PriorityActionBarServiceTest {

    @Test
    void higherPriorityActionBarPreemptsCurrentAndResumesItAfterCompletion() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeActionBarSender sender = new FakeActionBarSender();
        PriorityActionBarService service = new PriorityActionBarService(sender, scheduler, clock);

        service.show("player", "low", Component.text("low"), 1, Duration.ofSeconds(10), Duration.ZERO);
        clock.advance(Duration.ofSeconds(3));
        service.show("player", "high", Component.text("high"), 10, Duration.ofSeconds(2), Duration.ZERO);

        assertEquals(List.of(Component.text("low"), Component.text("high")), sender.shown("player"));
        assertTrue(scheduler.task("actionbar-player-low").cancelled());

        clock.advance(Duration.ofSeconds(2));
        scheduler.run("actionbar-player-high");

        assertEquals(List.of(Component.text("low"), Component.text("high"), Component.text("low")), sender.shown("player"));
        assertFalse(sender.wasCleared("player"));
    }

    @Test
    void lowerPriorityActionBarWaitsUntilCurrentCompletes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeActionBarSender sender = new FakeActionBarSender();
        PriorityActionBarService service = new PriorityActionBarService(sender, scheduler, clock);

        service.show("player", "high", Component.text("high"), 10, Duration.ofSeconds(1), Duration.ZERO);
        service.show("player", "low", Component.text("low"), 1, Duration.ofSeconds(3), Duration.ZERO);

        assertEquals(List.of(Component.text("high")), sender.shown("player"));

        clock.advance(Duration.ofSeconds(1));
        scheduler.run("actionbar-player-high");

        assertEquals(List.of(Component.text("high"), Component.text("low")), sender.shown("player"));
    }

    @Test
    void clearsAudienceWhenQueueIsEmptyAfterCompletion() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeActionBarSender sender = new FakeActionBarSender();
        PriorityActionBarService service = new PriorityActionBarService(sender, scheduler, clock);

        service.show("player", "only", Component.text("only"), 1, Duration.ofSeconds(1), Duration.ZERO);

        clock.advance(Duration.ofSeconds(1));
        scheduler.run("actionbar-player-only");

        assertTrue(sender.wasCleared("player"));
    }

    @Test
    void closeCancelsActiveTasksAndClearsVisibleActionBars() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeActionBarSender sender = new FakeActionBarSender();
        PriorityActionBarService service = new PriorityActionBarService(sender, scheduler, clock);

        service.show("player", "notice", Component.text("notice"), 1, Duration.ofSeconds(10), Duration.ZERO);
        service.close();

        assertTrue(scheduler.task("actionbar-player-notice").cancelled());
        assertTrue(sender.wasCleared("player"));
    }

    private static final class FakeActionBarSender implements ActionBarSender {
        private final Map<String, List<Component>> shown = new ConcurrentHashMap<>();
        private final List<String> cleared = new ArrayList<>();

        @Override
        public void show(String audienceId, Component message) {
            shown.computeIfAbsent(audienceId, ignored -> new ArrayList<>()).add(message);
        }

        @Override
        public void clear(String audienceId) {
            cleared.add(audienceId);
        }

        private List<Component> shown(String audienceId) {
            return shown.getOrDefault(audienceId, List.of());
        }

        private boolean wasCleared(String audienceId) {
            return cleared.contains(audienceId);
        }
    }

    private static final class FakeScheduler implements SchedulerPort {
        private final Map<String, FakeTask> tasks = new ConcurrentHashMap<>();

        @Override
        public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
            FakeTask scheduledTask = new FakeTask(taskId, task);
            tasks.put(taskId, scheduledTask);
            return scheduledTask;
        }

        @Override
        public ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
            throw new UnsupportedOperationException("Not needed by this test.");
        }

        private FakeTask task(String taskId) {
            return tasks.get(taskId);
        }

        private void run(String taskId) {
            tasks.get(taskId).run();
        }
    }

    private static final class FakeTask implements ScheduledTask {
        private final String id;
        private final Runnable task;
        private boolean cancelled;

        private FakeTask(String id, Runnable task) {
            this.id = id;
            this.task = task;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public void close() {
            cancelled = true;
        }

        private void run() {
            if (!cancelled) {
                task.run();
            }
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
