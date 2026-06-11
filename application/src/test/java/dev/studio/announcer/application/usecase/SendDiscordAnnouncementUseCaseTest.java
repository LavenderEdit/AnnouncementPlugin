package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DiscordBridgeService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class SendDiscordAnnouncementUseCaseTest {

    @Test
    void sendsAnnouncementToDiscordBridge() {
        FakeRepository repository = new FakeRepository();
        repository.announcement = Announcement.builder(AnnouncementId.of("event"), "Event")
                .messages(List.of("<gold>Event starts now</gold>"))
                .build();
        RecordingDiscordBridge bridge = new RecordingDiscordBridge(true);

        DiscordOutboundMessage message = new SendDiscordAnnouncementUseCase(repository, bridge)
                .execute(AnnouncementId.of("event"))
                .join();

        assertEquals("Event", message.title());
        assertEquals("<gold>Event starts now</gold>", message.description());
        assertEquals(List.of(message), bridge.sent);
    }

    @Test
    void rejectsDisabledDiscordBridge() {
        FakeRepository repository = new FakeRepository();
        repository.announcement = Announcement.builder(AnnouncementId.of("event"), "Event")
                .messages(List.of("Event"))
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> new SendDiscordAnnouncementUseCase(
                repository,
                new RecordingDiscordBridge(false))
                .execute(AnnouncementId.of("event")));

        assertEquals("Discord bridge is not enabled.", error.getMessage());
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

    private static final class RecordingDiscordBridge implements DiscordBridgeService {
        private final boolean enabled;
        private final List<DiscordOutboundMessage> sent = new ArrayList<>();

        private RecordingDiscordBridge(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public CompletableFuture<Void> send(DiscordOutboundMessage message) {
            sent.add(message);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void setInboundHandler(Consumer<dev.studio.announcer.api.discord.DiscordInboundMessage> handler) {
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public void close() {
        }
    }
}
