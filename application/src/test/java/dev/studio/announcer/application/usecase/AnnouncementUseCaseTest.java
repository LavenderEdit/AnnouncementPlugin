package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnnouncementUseCaseTest {

    @Test
    void createAnnouncementStoresValidAnnouncement() {
        FakeRepository repository = new FakeRepository();
        CreateAnnouncementUseCase useCase = new CreateAnnouncementUseCase(repository);
        Announcement announcement = announcement("launch");

        Announcement stored = useCase.execute(announcement);

        assertEquals(announcement, stored);
        assertEquals(Optional.of(announcement), repository.findById(AnnouncementId.of("launch")));
    }

    @Test
    void sendAnnouncementDispatchesExistingAnnouncement() {
        FakeRepository repository = new FakeRepository();
        FakeDispatcher dispatcher = new FakeDispatcher();
        Announcement announcement = announcement("network_alert");
        repository.save(announcement);

        new SendAnnouncementUseCase(repository, dispatcher).execute(AnnouncementId.of("network_alert"));

        assertEquals(List.of(announcement), dispatcher.broadcasted);
        assertTrue(dispatcher.previews.isEmpty());
    }

    @Test
    void sendAnnouncementFailsWhenIdDoesNotExist() {
        FakeRepository repository = new FakeRepository();
        FakeDispatcher dispatcher = new FakeDispatcher();

        assertThrows(AnnouncementNotFoundException.class,
                () -> new SendAnnouncementUseCase(repository, dispatcher).execute(AnnouncementId.of("missing")));
    }

    @Test
    void previewAnnouncementOnlyTargetsRequester() {
        FakeRepository repository = new FakeRepository();
        FakeDispatcher dispatcher = new FakeDispatcher();
        Announcement announcement = announcement("preview_me");
        repository.save(announcement);

        new PreviewAnnouncementUseCase(repository, dispatcher)
                .execute(AnnouncementId.of("preview_me"), "admin-uuid");

        assertTrue(dispatcher.broadcasted.isEmpty());
        assertEquals(List.of("admin-uuid:preview_me"), dispatcher.previews);
    }

    private static Announcement announcement(String id) {
        return Announcement.builder(AnnouncementId.of(id), "Announcement " + id)
                .type(AnnouncementType.GLOBAL)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Hello</green>"))
                .priority(1)
                .build();
    }

    private static final class FakeRepository implements AnnouncementRepository {
        private final Map<AnnouncementId, Announcement> announcements = new LinkedHashMap<>();

        @Override
        public Announcement save(Announcement announcement) {
            announcements.put(announcement.id(), announcement);
            return announcement;
        }

        @Override
        public Optional<Announcement> findById(AnnouncementId id) {
            return Optional.ofNullable(announcements.get(id));
        }

        @Override
        public List<Announcement> findAll() {
            return new ArrayList<>(announcements.values());
        }

        @Override
        public boolean deleteById(AnnouncementId id) {
            return announcements.remove(id) != null;
        }
    }

    private static final class FakeDispatcher implements AnnouncementDispatcher {
        private final List<Announcement> broadcasted = new ArrayList<>();
        private final List<String> previews = new ArrayList<>();

        @Override
        public DeliverySummary broadcast(Announcement announcement) {
            broadcasted.add(announcement);
            return new DeliverySummary(1, 0, 0);
        }

        @Override
        public DeliverySummary preview(Announcement announcement, String audienceId) {
            previews.add(audienceId + ":" + announcement.id().value());
            return new DeliverySummary(1, 0, 0);
        }
    }
}
