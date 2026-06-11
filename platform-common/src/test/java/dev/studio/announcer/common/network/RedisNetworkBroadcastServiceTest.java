package dev.studio.announcer.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class RedisNetworkBroadcastServiceTest {

    @Test
    void publishesEncodedPayloadAndDecodesInboundMessages() {
        FakeRedisPubSubClient client = new FakeRedisPubSubClient(true);
        RedisNetworkBroadcastService service = new RedisNetworkBroadcastService(
                client,
                new JacksonNetworkBroadcastCodec(),
                "advanced_announcer:broadcast");
        List<NetworkBroadcastRequest> inbound = new ArrayList<>();
        service.setHandler(inbound::add);

        NetworkBroadcastRequest request = new NetworkBroadcastRequest(
                "lobby-01",
                "survival",
                "announcer.receive.alert",
                "<green>Hello</green>",
                "CHAT",
                "");
        service.publish(request).join();
        client.emit(client.publishedPayloads.getFirst());

        assertEquals(List.of("advanced_announcer:broadcast"), client.publishedChannels);
        assertEquals(List.of(request), inbound);
    }

    @Test
    void closeUnsubscribesAndReportsDisconnectedClient() {
        FakeRedisPubSubClient client = new FakeRedisPubSubClient(false);
        RedisNetworkBroadcastService service = new RedisNetworkBroadcastService(
                client,
                new JacksonNetworkBroadcastCodec(),
                "advanced_announcer:broadcast");

        service.close();

        assertFalse(service.connected());
        assertEquals(1, client.closeCalls);
    }

    private static final class FakeRedisPubSubClient implements RedisPubSubClient {
        private final boolean connected;
        private final List<String> publishedChannels = new ArrayList<>();
        private final List<String> publishedPayloads = new ArrayList<>();
        private Consumer<String> subscriber = payload -> {
        };
        private int closeCalls;

        private FakeRedisPubSubClient(boolean connected) {
            this.connected = connected;
        }

        @Override
        public CompletableFuture<Void> publish(String channel, String payload) {
            publishedChannels.add(channel);
            publishedPayloads.add(payload);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void subscribe(String channel, Consumer<String> handler) {
            subscriber = handler;
        }

        void emit(String payload) {
            subscriber.accept(payload);
        }

        @Override
        public boolean connected() {
            return connected;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
