package dev.studio.announcer.api.service;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DiscordBridgeService extends AutoCloseable {

    CompletableFuture<Void> send(DiscordOutboundMessage message);

    void setInboundHandler(Consumer<DiscordInboundMessage> handler);

    boolean enabled();

    @Override
    void close();
}
