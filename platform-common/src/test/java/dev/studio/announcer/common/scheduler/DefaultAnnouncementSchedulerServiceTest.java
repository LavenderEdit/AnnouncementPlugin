package dev.studio.announcer.common.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class DefaultAnnouncementSchedulerServiceTest {

    @Test
    void schedulesEnabledAnnouncementsWithIntervalAndDispatchesWhenTaskRuns() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeDispatcher dispatcher = new FakeDispatcher();
        DefaultAnnouncementSchedulerService service = new DefaultAnnouncementSchedulerService(scheduler, dispatcher);
        Announcement announcement = announcement("sale", true, Duration.ofSeconds(30));

        service.reschedule(List.of(announcement));
        scheduler.run("announcement-sale");

        assertEquals(List.of("sale"), dispatcher.broadcasted);
    }

    @Test
    void ignoresDisabledAnnouncementsAndAnnouncementsWithoutInterval() {
        FakeScheduler scheduler = new FakeScheduler();
        DefaultAnnouncementSchedulerService service = new DefaultAnnouncementSchedulerService(scheduler, new FakeDispatcher());

        service.reschedule(List.of(
                announcement("disabled", false, Duration.ofSeconds(30)),
                announcement("manual", true, null)));

        assertTrue(scheduler.tasks.isEmpty());
    }

    @Test
    void rescheduleCancelsPreviousTasks() {
        FakeScheduler scheduler = new FakeScheduler();
        DefaultAnnouncementSchedulerService service = new DefaultAnnouncementSchedulerService(scheduler, new FakeDispatcher());

        service.reschedule(List.of(announcement("old", true, Duration.ofSeconds(30))));
        service.reschedule(List.of(announcement("new", true, Duration.ofSeconds(60))));

        assertTrue(scheduler.task("announcement-old").cancelled());
        assertTrue(scheduler.tasks.containsKey("announcement-new"));
    }

    @Test
    void stopCancelsAllTasks() {
        FakeScheduler scheduler = new FakeScheduler();
        DefaultAnnouncementSchedulerService service = new DefaultAnnouncementSchedulerService(scheduler, new FakeDispatcher());

        service.reschedule(List.of(announcement("sale", true, Duration.ofSeconds(30))));
        service.stop();

        assertTrue(scheduler.task("announcement-sale").cancelled());
    }

    private Announcement announcement(String id, boolean enabled, Duration interval) {
        Announcement.Builder builder = Announcement.builder(AnnouncementId.of(id), id)
                .enabled(enabled)
                .messages(List.of("<green>" + id + "</green>"));
        if (interval != null) {
            builder.interval(interval);
        }
        return builder.build();
    }

    private static final class FakeScheduler implements SchedulerPort {
        private final Map<String, FakeTask> tasks = new ConcurrentHashMap<>();

        @Override
        public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
            throw new UnsupportedOperationException("Not used.");
        }

        @Override
        public ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
            FakeTask scheduledTask = new FakeTask(taskId, task);
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
            task.run();
        }
    }

    private static final class FakeDispatcher implements AnnouncementDispatcher {
        private final List<String> broadcasted = new ArrayList<>();

        @Override
        public DeliverySummary broadcast(Announcement announcement) {
            broadcasted.add(announcement.id().value());
            return new DeliverySummary(1, 0, 0);
        }

        @Override
        public DeliverySummary preview(Announcement announcement, String audienceId) {
            return DeliverySummary.empty();
        }
    }
}
