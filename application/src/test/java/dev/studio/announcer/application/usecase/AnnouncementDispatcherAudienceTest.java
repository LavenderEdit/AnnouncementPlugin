package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import dev.studio.announcer.api.audience.ActorAudience;
import dev.studio.announcer.api.audience.AllAudience;
import dev.studio.announcer.api.audience.Audience;
import dev.studio.announcer.api.audience.OthersAudience;
import dev.studio.announcer.api.audience.PlayerAudience;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnnouncementDispatcherAudienceTest {

    @Test
    void broadcastDelegatesToDispatchWithAllAudience() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Announcement announcement = Announcement.builder(AnnouncementId.of("id1"), "Name").build();

        dispatcher.broadcast(announcement);

        assertEquals(1, dispatcher.calls.size());
        RecordingDispatcher.Call call = dispatcher.calls.get(0);
        assertSame(announcement, call.announcement);
        assertTrue(call.audience instanceof AllAudience);
        assertNull(call.actorId);
    }

    @Test
    void previewDelegatesToDispatchWithPlayerAudience() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Announcement announcement = Announcement.builder(AnnouncementId.of("id2"), "Name").build();

        dispatcher.preview(announcement, "player-uuid");

        assertEquals(1, dispatcher.calls.size());
        RecordingDispatcher.Call call = dispatcher.calls.get(0);
        assertSame(announcement, call.announcement);
        assertTrue(call.audience instanceof PlayerAudience);
        assertEquals("player-uuid", ((PlayerAudience) call.audience).playerId());
        assertNull(call.actorId);
    }

    @Test
    void dispatchWithActorIdResolvesAudienceFromMetadata() {
        // Test case 1: metadata audience is "actor"
        {
            RecordingDispatcher dispatcher = new RecordingDispatcher();
            Announcement announcement = Announcement.builder(AnnouncementId.of("id3"), "Name")
                    .metadata(Map.of("audience", "actor"))
                    .build();

            dispatcher.dispatch(announcement, "actor-uuid");

            assertEquals(1, dispatcher.calls.size());
            RecordingDispatcher.Call call = dispatcher.calls.get(0);
            assertTrue(call.audience instanceof ActorAudience);
            assertEquals("actor-uuid", call.actorId);
        }

        // Test case 2: metadata audience is "others"
        {
            RecordingDispatcher dispatcher = new RecordingDispatcher();
            Announcement announcement = Announcement.builder(AnnouncementId.of("id4"), "Name")
                    .metadata(Map.of("audience", "exclude_actor"))
                    .build();

            dispatcher.dispatch(announcement, "actor-uuid");

            assertEquals(1, dispatcher.calls.size());
            RecordingDispatcher.Call call = dispatcher.calls.get(0);
            assertTrue(call.audience instanceof OthersAudience);
            assertEquals("actor-uuid", call.actorId);
        }

        // Test case 3: default metadata (all)
        {
            RecordingDispatcher dispatcher = new RecordingDispatcher();
            Announcement announcement = Announcement.builder(AnnouncementId.of("id5"), "Name").build();

            dispatcher.dispatch(announcement, "actor-uuid");

            assertEquals(1, dispatcher.calls.size());
            RecordingDispatcher.Call call = dispatcher.calls.get(0);
            assertTrue(call.audience instanceof AllAudience);
            assertEquals("actor-uuid", call.actorId);
        }
    }

    private static final class RecordingDispatcher implements AnnouncementDispatcher {
        static record Call(Announcement announcement, Audience audience, String actorId) {}
        final List<Call> calls = new ArrayList<>();

        @Override
        public DeliverySummary dispatch(Announcement announcement, Audience audience, String actorId) {
            calls.add(new Call(announcement, audience, actorId));
            return new DeliverySummary(1, 0, 0);
        }
    }
}
