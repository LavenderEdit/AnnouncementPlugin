package dev.studio.announcer.application.service;

import static org.junit.jupiter.api.Assertions.*;

import dev.studio.announcer.api.service.AnnouncementManagementResult;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultAnnouncementManagementServiceTest {

    @Test
    void createValidAnnouncementSucceeds() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        Announcement announcement = sample("test");
        AnnouncementManagementResult result = service.create(announcement);

        assertTrue(result.success());
        assertTrue(repository.findById(AnnouncementId.of("test")).isPresent());
        assertEquals(1, scheduler.scheduled.size());
    }

    @Test
    void createDuplicateAnnouncementFails() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        service.create(sample("test"));
        AnnouncementManagementResult result = service.create(sample("test"));

        assertFalse(result.success());
        assertTrue(result.errors().getFirst().contains("already exists"));
    }

    @Test
    void updateExistingAnnouncementSucceeds() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        service.create(sample("test"));
        Announcement updated = sample("test").withEnabled(false);
        AnnouncementManagementResult result = service.update(updated);

        assertTrue(result.success());
        assertFalse(repository.findById(AnnouncementId.of("test")).orElseThrow().enabled());
    }

    @Test
    void updateNonExistingAnnouncementFails() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        AnnouncementManagementResult result = service.update(sample("nonexistent"));

        assertFalse(result.success());
        assertTrue(result.errors().getFirst().contains("not found"));
    }

    @Test
    void toggleAnnouncementSwitchesEnabled() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        service.create(sample("test"));
        AnnouncementManagementResult result = service.toggle(AnnouncementId.of("test"));

        assertTrue(result.success());
        assertFalse(repository.findById(AnnouncementId.of("test")).orElseThrow().enabled());
    }

    @Test
    void toggleNonExistingAnnouncementFails() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        AnnouncementManagementResult result = service.toggle(AnnouncementId.of("nonexistent"));

        assertFalse(result.success());
    }

    @Test
    void deleteExistingAnnouncementSucceeds() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        service.create(sample("test"));
        AnnouncementManagementResult result = service.delete(AnnouncementId.of("test"));

        assertTrue(result.success());
        assertTrue(repository.findById(AnnouncementId.of("test")).isEmpty());
        assertEquals(1, scheduler.cancelled.size());
    }

    @Test
    void deleteNonExistingAnnouncementFails() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        AnnouncementManagementResult result = service.delete(AnnouncementId.of("nonexistent"));

        assertFalse(result.success());
    }

    @Test
    void duplicateCreatesCopy() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        service.create(sample("original"));
        AnnouncementManagementResult result = service.duplicate(
                AnnouncementId.of("original"), AnnouncementId.of("copy"));

        assertTrue(result.success());
        assertTrue(repository.findById(AnnouncementId.of("copy")).isPresent());
        assertEquals("original Copy", repository.findById(AnnouncementId.of("copy")).orElseThrow().name());
    }

    @Test
    void duplicateNonExistingSourceFails() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        AnnouncementManagementResult result = service.duplicate(
                AnnouncementId.of("nonexistent"), AnnouncementId.of("copy"));

        assertFalse(result.success());
    }

    @Test
    void toggleScheduledAnnouncementWithIntervalSchedulesIt() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        Announcement announcement = sample("test").withEnabled(false);
        repository.save(announcement);
        service.toggle(AnnouncementId.of("test"));

        assertTrue(scheduler.scheduled.containsKey(AnnouncementId.of("test")));
    }

    @Test
    void toggleOffCancelsScheduledAnnouncement() {
        FakeRepository repository = new FakeRepository();
        FakeSchedulerService scheduler = new FakeSchedulerService();
        DefaultAnnouncementManagementService service = new DefaultAnnouncementManagementService(repository, scheduler);

        service.create(sample("test"));
        service.toggle(AnnouncementId.of("test"));

        assertTrue(scheduler.cancelled.contains(AnnouncementId.of("test")));
    }

    private static Announcement sample(String id) {
        return Announcement.builder(AnnouncementId.of(id), id)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hello"))
                .interval(Duration.ofSeconds(30))
                .build();
    }

    private static final class FakeRepository implements dev.studio.announcer.api.service.AnnouncementRepository {
        private final Map<AnnouncementId, Announcement> store = new HashMap<>();

        @Override public Announcement save(Announcement a) { store.put(a.id(), a); return a; }
        @Override public Optional<Announcement> findById(AnnouncementId id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Announcement> findAll() { return List.copyOf(store.values()); }
        @Override public boolean deleteById(AnnouncementId id) { return store.remove(id) != null; }
    }

    private static final class FakeSchedulerService implements AnnouncementSchedulerService {
        final Map<AnnouncementId, Announcement> scheduled = new HashMap<>();
        final java.util.Set<AnnouncementId> cancelled = new java.util.HashSet<>();

        @Override public void reschedule(java.util.Collection<Announcement> announcements) {
            announcements.forEach(a -> scheduled.put(a.id(), a));
        }
        @Override public void schedule(Announcement announcement) { scheduled.put(announcement.id(), announcement); }
        @Override public void cancel(AnnouncementId id) { cancelled.add(id); }
        @Override public boolean isScheduled(AnnouncementId id) { return scheduled.containsKey(id); }
        @Override public void stop() { scheduled.clear(); }
    }
}
