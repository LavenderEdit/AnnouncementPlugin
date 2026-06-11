package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HandleNetworkBroadcastUseCaseTest {

    @Test
    void ignoresRequestRejectedByFilter() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        HandleNetworkBroadcastUseCase useCase = new HandleNetworkBroadcastUseCase(dispatcher, request -> false);

        DeliverySummary summary = useCase.handle(new NetworkBroadcastRequest("proxy", "survival", "", "Hello", "CHAT", ""));

        assertEquals(DeliverySummary.empty(), summary);
        assertTrue(dispatcher.broadcasted.isEmpty());
    }

    @Test
    void convertsAcceptedPayloadToLocalAnnouncement() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        HandleNetworkBroadcastUseCase useCase = new HandleNetworkBroadcastUseCase(dispatcher, request -> true);

        DeliverySummary summary = useCase.handle(new NetworkBroadcastRequest(
                "message-1",
                "proxy",
                Set.of("lobby-01"),
                "",
                Set.of(),
                "announcer.receive.alert",
                "<green>Hello network</green>",
                "ACTIONBAR",
                "minecraft:block.note_block.pling",
                null));

        Announcement announcement = dispatcher.broadcasted.getFirst();
        assertEquals(new DeliverySummary(1, 0, 0), summary);
        assertEquals("network:message-1", announcement.id().value());
        assertEquals(Set.of(AnnouncementChannel.ACTIONBAR, AnnouncementChannel.SOUND), announcement.channels());
        assertEquals("announcer.receive.alert", announcement.permission().orElseThrow());
        assertTrue(announcement.actionBarOptions().isPresent());
        assertTrue(announcement.soundOptions().isPresent());
    }

    private static final class RecordingDispatcher implements AnnouncementDispatcher {
        private final List<Announcement> broadcasted = new ArrayList<>();

        @Override
        public DeliverySummary broadcast(Announcement announcement) {
            broadcasted.add(announcement);
            return new DeliverySummary(1, 0, 0);
        }

        @Override
        public DeliverySummary preview(Announcement announcement, String audienceId) {
            return DeliverySummary.empty();
        }
    }
}
