package dev.studio.announcer.spigot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class PriorityBossBarServiceTest {

    @Test
    void higherPriorityBossBarHidesCurrentAndResumesItAfterCompletion() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeBossBarSender sender = new FakeBossBarSender();
        PriorityBossBarService service = new PriorityBossBarService(sender, scheduler, clock);
        BossBar low = bar("low");
        BossBar high = bar("high");

        service.show("player", "low", low, 1, Duration.ofSeconds(10), false, true);
        clock.advance(Duration.ofSeconds(3));
        service.show("player", "high", high, 10, Duration.ofSeconds(2), false, true);

        assertEquals(List.of("show:low", "hide:low", "show:high"), sender.events("player"));
        assertTrue(scheduler.task("bossbar-finish-player-low").cancelled());

        clock.advance(Duration.ofSeconds(2));
        scheduler.run("bossbar-finish-player-high");

        assertEquals(List.of("show:low", "hide:low", "show:high", "hide:high", "show:low"), sender.events("player"));
    }

    @Test
    void animatedBossBarSchedulesProgressTaskAndCancelsItWhenFinished() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeBossBarSender sender = new FakeBossBarSender();
        PriorityBossBarService service = new PriorityBossBarService(sender, scheduler, clock);

        service.show("player", "animated", bar("animated"), 1, Duration.ofSeconds(5), true, true);

        assertTrue(scheduler.task("bossbar-progress-player-animated").repeating());

        clock.advance(Duration.ofSeconds(5));
        scheduler.run("bossbar-finish-player-animated");

        assertTrue(scheduler.task("bossbar-progress-player-animated").cancelled());
        assertEquals(List.of("show:animated", "hide:animated"), sender.events("player"));
    }

    @Test
    void autoHideFalseKeepsBossBarVisibleUntilServiceCloses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeBossBarSender sender = new FakeBossBarSender();
        PriorityBossBarService service = new PriorityBossBarService(sender, scheduler, clock);

        service.show("player", "persistent", bar("persistent"), 1, Duration.ofSeconds(1), false, false);

        clock.advance(Duration.ofSeconds(1));
        scheduler.run("bossbar-finish-player-persistent");

        assertEquals(List.of("show:persistent"), sender.events("player"));

        service.close();

        assertEquals(List.of("show:persistent", "hide:persistent"), sender.events("player"));
    }

    @Test
    void closeCancelsTasksAndHidesVisibleBossBars() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-10T12:00:00Z"));
        FakeScheduler scheduler = new FakeScheduler();
        FakeBossBarSender sender = new FakeBossBarSender();
        PriorityBossBarService service = new PriorityBossBarService(sender, scheduler, clock);

        service.show("player", "notice", bar("notice"), 1, Duration.ofSeconds(10), true, true);
        service.close();

        assertTrue(scheduler.task("bossbar-finish-player-notice").cancelled());
        assertTrue(scheduler.task("bossbar-progress-player-notice").cancelled());
        assertEquals(List.of("show:notice", "hide:notice"), sender.events("player"));
    }

    private BossBar bar(String name) {
        return BossBar.bossBar(Component.text(name), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    }

    private static final class FakeBossBarSender implements BossBarSender {
        private final Map<String, List<String>> events = new ConcurrentHashMap<>();

        @Override
        public void show(String audienceId, BossBar bossBar) {
            events.computeIfAbsent(audienceId, ignored -> new ArrayList<>()).add("show:" + label(bossBar));
        }

        @Override
        public void hide(String audienceId, BossBar bossBar) {
            events.computeIfAbsent(audienceId, ignored -> new ArrayList<>()).add("hide:" + label(bossBar));
        }

        private List<String> events(String audienceId) {
            return events.getOrDefault(audienceId, List.of());
        }

        private String label(BossBar bossBar) {
            return ((net.kyori.adventure.text.TextComponent) bossBar.name()).content();
        }
    }

    private static final class FakeScheduler implements SchedulerPort {
        private final Map<String, FakeTask> tasks = new ConcurrentHashMap<>();

        @Override
        public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
            FakeTask scheduledTask = new FakeTask(taskId, task, false);
            tasks.put(taskId, scheduledTask);
            return scheduledTask;
        }

        @Override
        public ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
            FakeTask scheduledTask = new FakeTask(taskId, task, true);
            tasks.put(taskId, scheduledTask);
            return scheduledTask;
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
        private final boolean repeating;
        private boolean cancelled;

        private FakeTask(String id, Runnable task, boolean repeating) {
            this.id = id;
            this.task = task;
            this.repeating = repeating;
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

        private boolean repeating() {
            return repeating;
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
