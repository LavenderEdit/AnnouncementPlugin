package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementExecutionContext;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExecuteTriggeredAnnouncementsUseCaseTest {

    @Test
    void executesMatchingEnabledAnnouncementsWithDelay() {
        FakeRepository repository = new FakeRepository();
        Announcement announcement = Announcement.builder(AnnouncementId.of("join_msg"), "Join Message")
                .type(AnnouncementType.EVENT_JOIN)
                .enabled(true)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Welcome!"))
                .metadata(Map.of("join-delay", "PT1S"))
                .build();
        repository.save(announcement);

        FakeDispatcher dispatcher = new FakeDispatcher();
        FakeScheduler scheduler = new FakeScheduler();
        TriggerMatcher matcher = new TriggerMatcher("server-01", Set.of("default"));

        ExecuteTriggeredAnnouncementsUseCase useCase = new ExecuteTriggeredAnnouncementsUseCase(
                repository, dispatcher, scheduler, matcher, "join-delay", Duration.ofSeconds(5)
        );

        AnnouncementExecutionContext context = new AnnouncementExecutionContext("player-uuid", Map.of());
        useCase.execute(AnnouncementType.EVENT_JOIN, context);

        assertEquals(1, scheduler.tasks.size());
        FakeScheduler.Task task = scheduler.tasks.get(0);
        assertEquals("triggered-announcement-join_msg-player-uuid", task.taskId);
        assertEquals(Duration.ofSeconds(1), task.delay); // Metadata delay overrides default

        // Run the task and verify dispatch
        task.runnable.run();
        assertEquals(1, dispatcher.dispatched.size());
        assertEquals("join_msg", dispatcher.dispatched.get(0).announcement.id().value());
        assertEquals("player-uuid", dispatcher.dispatched.get(0).actorId);
    }

    @Test
    void ignoresDisabledOrMismatchedAnnouncements() {
        FakeRepository repository = new FakeRepository();
        Announcement disabled = Announcement.builder(AnnouncementId.of("disabled"), "Disabled")
                .type(AnnouncementType.EVENT_JOIN)
                .enabled(false)
                .build();
        Announcement wrongType = Announcement.builder(AnnouncementId.of("wrong"), "Wrong Type")
                .type(AnnouncementType.GLOBAL)
                .enabled(true)
                .build();
        repository.save(disabled);
        repository.save(wrongType);

        FakeDispatcher dispatcher = new FakeDispatcher();
        FakeScheduler scheduler = new FakeScheduler();
        TriggerMatcher matcher = new TriggerMatcher("server-01", Set.of("default"));

        ExecuteTriggeredAnnouncementsUseCase useCase = new ExecuteTriggeredAnnouncementsUseCase(
                repository, dispatcher, scheduler, matcher, "join-delay", Duration.ZERO
        );

        useCase.execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext("player-uuid", Map.of()));

        assertTrue(scheduler.tasks.isEmpty());
    }

    private static final class FakeRepository implements AnnouncementRepository {
        private final Map<AnnouncementId, Announcement> store = new HashMap<>();

        @Override
        public Announcement save(Announcement announcement) {
            store.put(announcement.id(), announcement);
            return announcement;
        }

        @Override
        public Optional<Announcement> findById(AnnouncementId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Announcement> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public boolean deleteById(AnnouncementId id) {
            return store.remove(id) != null;
        }
    }

    private static final class FakeDispatcher implements AnnouncementDispatcher {
        static record DispatchCall(Announcement announcement, String actorId) {}
        final List<DispatchCall> dispatched = new ArrayList<>();

        @Override
        public DeliverySummary broadcast(Announcement announcement) {
            return DeliverySummary.empty();
        }

        @Override
        public DeliverySummary preview(Announcement announcement, String audienceId) {
            return DeliverySummary.empty();
        }

        @Override
        public DeliverySummary dispatch(Announcement announcement, String actorId) {
            dispatched.add(new DispatchCall(announcement, actorId));
            return new DeliverySummary(1, 0, 0);
        }
    }

    private static final class FakeScheduler implements SchedulerPort {
        static record Task(String taskId, Duration delay, Runnable runnable) {}
        final List<Task> tasks = new ArrayList<>();

        @Override
        public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
            tasks.add(new Task(taskId, delay, task));
            return new ScheduledTask() {
                @Override public String id() { return taskId; }
                @Override public boolean cancelled() { return false; }
                @Override public void close() {}
            };
        }

        @Override
        public ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
            return new ScheduledTask() {
                @Override public String id() { return taskId; }
                @Override public boolean cancelled() { return false; }
                @Override public void close() {}
            };
        }
    }
}
