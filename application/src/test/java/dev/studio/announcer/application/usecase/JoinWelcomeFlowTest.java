package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import dev.studio.announcer.api.audience.ActorAudience;
import dev.studio.announcer.api.audience.AllAudience;
import dev.studio.announcer.api.audience.Audience;
import dev.studio.announcer.api.audience.OthersAudience;
import dev.studio.announcer.api.audience.PlayerAudience;
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

/**
 * Cubre la resolucion de triggers, resolucion de audiencia, contexto actor/receptor,
 * omision de anuncios deshabilitados, prevencion de ejecuciones duplicadas y
 * aislamiento de errores en el flujo de bienvenida (nivel de caso de uso).
 *
 * <p>La condicion de primera entrada ({@code join-state}) se evalua en el dispatcher
 * (runtime de Spigot); aqui se valida que el caso de uso programa los anuncios
 * EVENT_JOIN habilitados y transporta la condicion hasta el dispatcher.</p>
 */
class JoinWelcomeFlowTest {

    private static final String SERVER_ID = "server-01";
    private static final Set<String> GROUPS = Set.of("default");

    private static Announcement joinAnnouncement(String id, boolean enabled, String joinStateCondition) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("audience", "all");
        return Announcement.builder(AnnouncementId.of(id), id)
                .type(AnnouncementType.EVENT_JOIN)
                .enabled(enabled)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Welcome"))
                .conditions(joinStateCondition == null ? List.of() : List.of(joinStateCondition))
                .metadata(metadata)
                .build();
    }

    private static ExecuteTriggeredAnnouncementsUseCase useCase(
            AnnouncementRepository repository, RecordingDispatcher dispatcher, RecordingScheduler scheduler) {
        return new ExecuteTriggeredAnnouncementsUseCase(
                repository, dispatcher, scheduler, new TriggerMatcher(SERVER_ID, GROUPS), Duration.ZERO);
    }

    @Test
    void schedulesEnabledEventJoinAnnouncementsWithTheirConditions() {
        FakeRepository repository = new FakeRepository();
        repository.save(joinAnnouncement("first_join", true, "join-state: FIRST_JOIN"));
        repository.save(joinAnnouncement("recurring", true, "join-state: RECURRING"));

        RecordingDispatcher dispatcher = new RecordingDispatcher();
        RecordingScheduler scheduler = new RecordingScheduler();
        useCase(repository, dispatcher, scheduler)
                .execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext("alice", Map.of()));

        // Both enabled EVENT_JOIN announcements are scheduled; join-state is evaluated later by the dispatcher
        assertEquals(2, scheduler.tasks.size());
        boolean hasFirst = scheduler.tasks.stream().anyMatch(t -> t.taskId.equals("triggered-announcement-first_join-alice"));
        boolean hasRecurring = scheduler.tasks.stream().anyMatch(t -> t.taskId.equals("triggered-announcement-recurring-alice"));
        assertTrue(hasFirst);
        assertTrue(hasRecurring);
    }

    @Test
    void skipsDisabledEventJoinAnnouncement() {
        FakeRepository repository = new FakeRepository();
        repository.save(joinAnnouncement("first_join", false, "join-state: FIRST_JOIN"));
        repository.save(joinAnnouncement("recurring", true, "join-state: RECURRING"));

        RecordingDispatcher dispatcher = new RecordingDispatcher();
        RecordingScheduler scheduler = new RecordingScheduler();
        useCase(repository, dispatcher, scheduler)
                .execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext("alice", Map.of()));

        assertEquals(1, scheduler.tasks.size());
        assertEquals("triggered-announcement-recurring-alice", scheduler.tasks.get(0).taskId);
    }

    @Test
    void ignoresNonJoinAnnouncements() {
        FakeRepository repository = new FakeRepository();
        repository.save(Announcement.builder(AnnouncementId.of("global"), "Global")
                .type(AnnouncementType.GLOBAL)
                .enabled(true)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hi"))
                .build());

        RecordingDispatcher dispatcher = new RecordingDispatcher();
        RecordingScheduler scheduler = new RecordingScheduler();
        useCase(repository, dispatcher, scheduler)
                .execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext("alice", Map.of()));

        assertTrue(scheduler.tasks.isEmpty());
    }

    @Test
    void resolvesAudienceFromMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("audience", "actor");
        Announcement announcement = Announcement.builder(AnnouncementId.of("pm"), "PM")
                .type(AnnouncementType.EVENT_JOIN)
                .enabled(true)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hi"))
                .metadata(metadata)
                .build();
        FakeRepository repository = new FakeRepository();
        repository.save(announcement);

        RecordingDispatcher dispatcher = new RecordingDispatcher();
        RecordingScheduler scheduler = new RecordingScheduler();
        useCase(repository, dispatcher, scheduler)
                .execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext("alice", Map.of()));

        scheduler.tasks.get(0).runnable.run();
        assertEquals(1, dispatcher.calls.size());
        assertTrue(dispatcher.calls.get(0).audience instanceof ActorAudience);
    }

    @Test
    void reloadDoesNotDuplicateExecutionForSameActor() {
        FakeRepository repository = new FakeRepository();
        repository.save(joinAnnouncement("welcome", true, null));

        RecordingDispatcher dispatcher = new RecordingDispatcher();
        RecordingScheduler scheduler = new RecordingScheduler();
        ExecuteTriggeredAnnouncementsUseCase useCase = useCase(repository, dispatcher, scheduler);

        AnnouncementExecutionContext context = new AnnouncementExecutionContext("alice", Map.of());
        useCase.execute(AnnouncementType.EVENT_JOIN, context);
        useCase.execute(AnnouncementType.EVENT_JOIN, context);

        // Same actorId yields the same taskId, so the scheduler keeps a single task
        assertEquals(1, scheduler.tasks.size());
        assertEquals("triggered-announcement-welcome-alice", scheduler.tasks.get(0).taskId);

        scheduler.tasks.get(0).runnable.run();
        assertEquals(1, dispatcher.calls.size());
    }

    @Test
    void errorInOneAnnouncementDoesNotPreventOthers() {
        FakeRepository repository = new FakeRepository();
        repository.save(joinAnnouncement("broken", true, null));
        repository.save(joinAnnouncement("ok", true, null));

        RecordingDispatcher dispatcher = new RecordingDispatcher() {
            @Override
            public DeliverySummary dispatch(Announcement announcement, Audience audience, String actorId) {
                if (announcement.id().value().equals("broken")) {
                    throw new IllegalStateException("simulated delivery failure");
                }
                return super.dispatch(announcement, audience, actorId);
            }
        };
        RecordingScheduler scheduler = new RecordingScheduler();
        useCase(repository, dispatcher, scheduler)
                .execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext("alice", Map.of()));

        assertEquals(2, scheduler.tasks.size());
        scheduler.tasks.forEach(task -> task.runnable.run());
        assertEquals(1, dispatcher.calls.size());
        assertEquals("ok", dispatcher.calls.get(0).announcement.id().value());
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

    private static class RecordingDispatcher implements AnnouncementDispatcher {
        record Call(Announcement announcement, Audience audience, String actorId) {}
        final List<Call> calls = new ArrayList<>();

        @Override
        public DeliverySummary dispatch(Announcement announcement, Audience audience, String actorId) {
            calls.add(new Call(announcement, audience, actorId));
            return new DeliverySummary(1, 0, 0);
        }
    }

    private static class RecordingScheduler implements SchedulerPort {
        record Task(String taskId, Duration delay, Runnable runnable) {}
        final List<Task> tasks = new ArrayList<>();
        private final Map<String, Task> byId = new HashMap<>();

        @Override
        public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
            Task existing = byId.get(taskId);
            if (existing != null) {
                tasks.remove(existing);
            }
            Task created = new Task(taskId, delay, task);
            byId.put(taskId, created);
            tasks.add(created);
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
