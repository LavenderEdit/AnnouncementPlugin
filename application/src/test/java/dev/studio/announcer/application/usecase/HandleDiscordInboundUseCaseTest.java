package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HandleDiscordInboundUseCaseTest {

    @Test
    void formatsDiscordMessageAsLocalChatAnnouncement() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        HandleDiscordInboundUseCase useCase = new HandleDiscordInboundUseCase(
                dispatcher,
                "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>");

        DeliverySummary summary = useCase.handle(new DiscordInboundMessage(
                "123",
                "456",
                "Lavender",
                "Server restart in 5",
                Set.of("admin"),
                Instant.parse("2026-06-11T10:00:00Z")));

        Announcement announcement = dispatcher.broadcasted.getFirst();
        assertEquals(new DeliverySummary(1, 0, 0), summary);
        assertEquals(AnnouncementType.DISCORD, announcement.type());
        assertEquals(Set.of(AnnouncementChannel.CHAT), announcement.channels());
        assertEquals("<aqua>Lavender</aqua>: <white>Server restart in 5</white>", announcement.messages().getFirst());
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
