package dev.studio.announcer.common.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NetworkTargetFilterTest {

    @Test
    void ignoresSelfOriginWhenConfigured() {
        NetworkTargetFilter filter = new NetworkTargetFilter("lobby-01", Set.of("lobby"), true);

        assertFalse(filter.accepts(new NetworkBroadcastRequest(
                "lobby-01",
                "lobby",
                "",
                "Hello",
                "CHAT",
                "")));
    }

    @Test
    void acceptsMatchingTargetServerAndGroup() {
        NetworkTargetFilter filter = new NetworkTargetFilter("survival-01", Set.of("survival"), true);
        NetworkBroadcastRequest request = new NetworkBroadcastRequest(
                "message-1",
                "lobby-01",
                Set.of("survival-01"),
                "",
                Set.of("survival"),
                "",
                "Hello",
                "CHAT",
                "",
                null);

        assertTrue(filter.accepts(request));
    }

    @Test
    void rejectsNonMatchingTargetServerOrGroup() {
        NetworkTargetFilter filter = new NetworkTargetFilter("lobby-01", Set.of("lobby"), true);

        assertFalse(filter.accepts(new NetworkBroadcastRequest(
                "message-1",
                "proxy",
                Set.of("survival-01"),
                "",
                Set.of(),
                "",
                "Hello",
                "CHAT",
                "",
                null)));
        assertFalse(filter.accepts(new NetworkBroadcastRequest(
                "message-2",
                "proxy",
                Set.of(),
                "survival",
                Set.of(),
                "",
                "Hello",
                "CHAT",
                "",
                null)));
    }
}
