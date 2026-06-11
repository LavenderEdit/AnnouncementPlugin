package dev.studio.announcer.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JacksonNetworkBroadcastCodecTest {

    @Test
    void decodesOriginalDocumentPayloadShape() {
        JacksonNetworkBroadcastCodec codec = new JacksonNetworkBroadcastCodec();

        NetworkBroadcastRequest request = codec.decode("""
                {
                  "originServer": "lobby-01",
                  "targetGroup": "survival_servers",
                  "permission": "announcer.receive.alert",
                  "content": "<gradient:gold:yellow>Evento de XP Doble iniciado</gradient>",
                  "packetType": "TOAST",
                  "sound": "ui.toast.challenge_complete"
                }
                """);

        assertEquals("lobby-01", request.originServer());
        assertEquals("survival_servers", request.targetGroup());
        assertEquals(Set.of("survival_servers"), request.targetGroups());
        assertEquals("TOAST", request.packetType());
        assertTrue(request.content().contains("XP Doble"));
    }

    @Test
    void roundTripsV1Payload() {
        JacksonNetworkBroadcastCodec codec = new JacksonNetworkBroadcastCodec();
        NetworkBroadcastRequest request = new NetworkBroadcastRequest(
                "message-1",
                "lobby-01",
                Set.of("survival-01"),
                "",
                Set.of("survival"),
                "announcer.receive.alert",
                "<green>Hello</green>",
                "CHAT",
                "minecraft:block.note_block.pling",
                Instant.parse("2026-06-11T10:00:00Z"));

        NetworkBroadcastRequest decoded = codec.decode(codec.encode(request));

        assertEquals(request, decoded);
    }
}
