package dev.studio.announcer.common.network;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class LettuceRedisPubSubClient implements RedisPubSubClient {
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> publisher;
    private final StatefulRedisPubSubConnection<String, String> subscriber;

    public LettuceRedisPubSubClient(String uri) {
        RedisURI redisUri = RedisURI.create(uri == null || uri.isBlank() ? "redis://localhost:6379" : uri.trim());
        this.client = RedisClient.create(redisUri);
        this.client.setOptions(ClientOptions.builder().autoReconnect(true).build());
        this.publisher = client.connect();
        this.subscriber = client.connectPubSub();
    }

    @Override
    public CompletableFuture<Void> publish(String channel, String payload) {
        return publisher.async().publish(channel, payload).toCompletableFuture().thenApply(ignored -> null);
    }

    @Override
    public void subscribe(String channel, Consumer<String> handler) {
        Consumer<String> safeHandler = Objects.requireNonNullElse(handler, payload -> {
        });
        subscriber.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String receivedChannel, String message) {
                if (channel.equals(receivedChannel)) {
                    safeHandler.accept(message);
                }
            }
        });
        subscriber.async().subscribe(channel);
    }

    @Override
    public boolean connected() {
        return publisher.isOpen() && subscriber.isOpen();
    }

    @Override
    public void close() {
        subscriber.close();
        publisher.close();
        client.shutdown();
    }
}
