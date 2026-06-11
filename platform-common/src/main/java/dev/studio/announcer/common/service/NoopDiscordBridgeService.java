package dev.studio.announcer.common.service;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import dev.studio.announcer.api.service.DiscordBridgeService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class NoopDiscordBridgeService implements DiscordBridgeService {
    private Consumer<DiscordInboundMessage> handler = message -> {
    };

    @Override
    public CompletableFuture<Void> send(DiscordOutboundMessage message) {
        return CompletableFuture.failedFuture(new IllegalStateException("Discord bridge is disabled."));
    }

    @Override
    public void setInboundHandler(Consumer<DiscordInboundMessage> handler) {
        this.handler = handler == null ? message -> {
        } : handler;
    }

    public void emit(DiscordInboundMessage message) {
        handler.accept(message);
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public void close() {
        handler = message -> {
        };
    }
}
