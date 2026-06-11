package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.NetworkBroadcastService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class BroadcastAnnouncementUseCaseTest {

    @Test
    void publishesExistingAnnouncementAsNetworkPayload() {
        FakeRepository repository = new FakeRepository();
        repository.announcement = Announcement.builder(AnnouncementId.of("xp"), "Double XP")
                .channels(EnumSet.of(AnnouncementChannel.TOAST))
                .messages(List.of("<gold>Double XP</gold>"))
                .permission("announcer.receive.alert")
                .build();
        RecordingNetworkBroadcastService network = new RecordingNetworkBroadcastService(true);

        NetworkBroadcastRequest request = new BroadcastAnnouncementUseCase(
                repository,
                network,
                "lobby-01",
                java.util.Set.of("survival"))
                .execute(AnnouncementId.of("xp"))
                .join();

        assertEquals("lobby-01", request.originServer());
        assertEquals(java.util.Set.of("survival"), request.targetGroups());
        assertEquals("announcer.receive.alert", request.permission());
        assertEquals("<gold>Double XP</gold>", request.content());
        assertEquals("TOAST", request.packetType());
        assertEquals(List.of(request), network.published);
    }

    @Test
    void rejectsDisconnectedNetworkService() {
        FakeRepository repository = new FakeRepository();
        repository.announcement = Announcement.builder(AnnouncementId.of("xp"), "Double XP")
                .messages(List.of("Double XP"))
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> new BroadcastAnnouncementUseCase(
                repository,
                new RecordingNetworkBroadcastService(false),
                "lobby-01",
                java.util.Set.of())
                .execute(AnnouncementId.of("xp")));

        assertEquals("Network broadcast service is not connected.", error.getMessage());
    }

    @Test
    void rejectsMissingAnnouncement() {
        AnnouncementNotFoundException error = assertThrows(AnnouncementNotFoundException.class, () -> new BroadcastAnnouncementUseCase(
                new FakeRepository(),
                new RecordingNetworkBroadcastService(true),
                "lobby-01",
                java.util.Set.of())
                .execute(AnnouncementId.of("missing")));

        assertEquals("Announcement not found: missing", error.getMessage());
    }

    private static final class FakeRepository implements AnnouncementRepository {
        private Announcement announcement;

        @Override
        public Announcement save(Announcement announcement) {
            this.announcement = announcement;
            return announcement;
        }

        @Override
        public Optional<Announcement> findById(AnnouncementId id) {
            return Optional.ofNullable(announcement)
                    .filter(value -> value.id().equals(id));
        }

        @Override
        public List<Announcement> findAll() {
            return announcement == null ? List.of() : List.of(announcement);
        }

        @Override
        public boolean deleteById(AnnouncementId id) {
            return false;
        }
    }

    private static final class RecordingNetworkBroadcastService implements NetworkBroadcastService {
        private final boolean connected;
        private final List<NetworkBroadcastRequest> published = new ArrayList<>();

        private RecordingNetworkBroadcastService(boolean connected) {
            this.connected = connected;
        }

        @Override
        public CompletableFuture<Void> publish(NetworkBroadcastRequest request) {
            published.add(request);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void setHandler(Consumer<NetworkBroadcastRequest> handler) {
        }

        @Override
        public boolean connected() {
            return connected;
        }

        @Override
        public void close() {
        }
    }
}
