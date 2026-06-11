package dev.studio.announcer.api.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NetworkBroadcastRequestTest {

    @Test
    void oldPayloadShapeStillCreatesV1Request() {
        NetworkBroadcastRequest request = new NetworkBroadcastRequest(
                " lobby-01 ",
                " survival_servers ",
                " announcer.receive.alert ",
                " <gold>Double XP</gold> ",
                " TOAST ",
                " ui.toast.challenge_complete ");

        assertFalse(request.messageId().isBlank());
        assertEquals("lobby-01", request.originServer());
        assertEquals("survival_servers", request.targetGroup());
        assertEquals(Set.of("survival_servers"), request.targetGroups());
        assertEquals(Set.of(), request.targetServers());
        assertEquals("announcer.receive.alert", request.permission());
        assertEquals("<gold>Double XP</gold>", request.content());
        assertEquals("TOAST", request.packetType());
        assertEquals("ui.toast.challenge_complete", request.sound());
        assertTrue(request.createdAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void v1PayloadNormalizesCollectionsAndDefaults() {
        NetworkBroadcastRequest request = new NetworkBroadcastRequest(
                "message-1",
                "proxy-01",
                Set.of(" lobby-01 ", "", "survival-01"),
                "",
                Set.of(" survival ", "lobby", " "),
                "",
                "Hello",
                "",
                "",
                Instant.parse("2026-06-11T10:00:00Z"));

        assertEquals("message-1", request.messageId());
        assertEquals(Set.of("lobby-01", "survival-01"), request.targetServers());
        assertEquals(Set.of("survival", "lobby"), request.targetGroups());
        assertEquals("CHAT", request.packetType());
        assertEquals(Instant.parse("2026-06-11T10:00:00Z"), request.createdAt());
    }
}
