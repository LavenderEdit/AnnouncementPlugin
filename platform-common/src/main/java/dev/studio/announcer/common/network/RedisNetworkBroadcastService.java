package dev.studio.announcer.common.network;

import dev.studio.announcer.api.network.NetworkBroadcastCodec;
import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import dev.studio.announcer.api.service.NetworkBroadcastService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class RedisNetworkBroadcastService implements NetworkBroadcastService {
    private final RedisPubSubClient client;
    private final NetworkBroadcastCodec codec;
    private final String channel;
    private Consumer<NetworkBroadcastRequest> handler = request -> {
    };

    public RedisNetworkBroadcastService(
            RedisPubSubClient client,
            NetworkBroadcastCodec codec,
            String channel) {
        this.client = Objects.requireNonNull(client, "client");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.channel = channel == null || channel.isBlank() ? "advanced_announcer:broadcast" : channel.trim();
        this.client.subscribe(this.channel, this::handlePayload);
    }

    @Override
    public CompletableFuture<Void> publish(NetworkBroadcastRequest request) {
        return client.publish(channel, codec.encode(request));
    }

    @Override
    public void setHandler(Consumer<NetworkBroadcastRequest> handler) {
        this.handler = handler == null ? request -> {
        } : handler;
    }

    @Override
    public boolean connected() {
        return client.connected();
    }

    @Override
    public void close() {
        handler = request -> {
        };
        client.close();
    }

    private void handlePayload(String payload) {
        try {
            handler.accept(codec.decode(payload));
        } catch (IllegalArgumentException ignored) {
            // Invalid network messages are ignored to keep the subscription alive.
        }
    }
}
