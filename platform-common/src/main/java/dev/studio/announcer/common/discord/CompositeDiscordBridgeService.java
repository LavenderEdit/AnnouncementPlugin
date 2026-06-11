package dev.studio.announcer.common.discord;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import dev.studio.announcer.api.service.DiscordBridgeService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class CompositeDiscordBridgeService implements DiscordBridgeService {
    private final DiscordBridgeService outbound;
    private final DiscordBridgeService inbound;

    public CompositeDiscordBridgeService(DiscordBridgeService outbound, DiscordBridgeService inbound) {
        this.outbound = Objects.requireNonNull(outbound, "outbound");
        this.inbound = Objects.requireNonNull(inbound, "inbound");
    }

    @Override
    public CompletableFuture<Void> send(DiscordOutboundMessage message) {
        return outbound.send(message);
    }

    @Override
    public void setInboundHandler(Consumer<DiscordInboundMessage> handler) {
        inbound.setInboundHandler(handler);
    }

    @Override
    public boolean enabled() {
        return outbound.enabled() || inbound.enabled();
    }

    @Override
    public void close() {
        inbound.close();
        outbound.close();
    }
}
