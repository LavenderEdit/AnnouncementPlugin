package dev.studio.announcer.common.network;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface RedisPubSubClient extends AutoCloseable {

    CompletableFuture<Void> publish(String channel, String payload);

    void subscribe(String channel, Consumer<String> handler);

    boolean connected();

    @Override
    void close();
}
