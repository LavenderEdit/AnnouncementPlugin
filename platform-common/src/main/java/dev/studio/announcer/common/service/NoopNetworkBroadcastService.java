package dev.studio.announcer.common.service;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import dev.studio.announcer.api.service.NetworkBroadcastService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class NoopNetworkBroadcastService implements NetworkBroadcastService {
    private Consumer<NetworkBroadcastRequest> handler = request -> {
    };

    @Override
    public CompletableFuture<Void> publish(NetworkBroadcastRequest request) {
        handler.accept(request);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void setHandler(Consumer<NetworkBroadcastRequest> handler) {
        this.handler = handler == null ? request -> {
        } : handler;
    }

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public void close() {
        handler = request -> {
        };
    }
}
