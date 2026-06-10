package dev.studio.announcer.api.service;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface NetworkBroadcastService extends AutoCloseable {

    CompletableFuture<Void> publish(NetworkBroadcastRequest request);

    void setHandler(Consumer<NetworkBroadcastRequest> handler);

    boolean connected();

    @Override
    void close();
}
