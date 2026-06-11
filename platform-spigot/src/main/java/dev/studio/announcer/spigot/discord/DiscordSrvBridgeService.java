package dev.studio.announcer.spigot.discord;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import dev.studio.announcer.api.service.DiscordBridgeService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class DiscordSrvBridgeService implements DiscordBridgeService {
    private final DiscordSrvSettings settings;
    private final Map<String, Instant> lastAcceptedByUser = new HashMap<>();
    private Consumer<DiscordInboundMessage> inboundHandler = message -> {
    };

    public DiscordSrvBridgeService(DiscordSrvSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public CompletableFuture<Void> send(DiscordOutboundMessage message) {
        return CompletableFuture.failedFuture(new IllegalStateException("DiscordSRV bridge is inbound-only."));
    }

    @Override
    public void setInboundHandler(Consumer<DiscordInboundMessage> handler) {
        inboundHandler = handler == null ? message -> {
        } : handler;
    }

    public void acceptInbound(DiscordInboundMessage message) {
        if (!enabled() || !allowedChannel(message) || !allowedRole(message) || !cooldownElapsed(message)) {
            return;
        }
        lastAcceptedByUser.put(message.userId(), message.createdAt());
        inboundHandler.accept(message);
    }

    @Override
    public boolean enabled() {
        return settings.enabled();
    }

    @Override
    public void close() {
        inboundHandler = message -> {
        };
        lastAcceptedByUser.clear();
    }

    public String minecraftFormat() {
        return settings.minecraftFormat();
    }

    private boolean allowedChannel(DiscordInboundMessage message) {
        return settings.channelWhitelist().isEmpty() || settings.channelWhitelist().contains(message.channelId());
    }

    private boolean allowedRole(DiscordInboundMessage message) {
        return settings.allowedRoleIds().isEmpty()
                || message.roleIds().stream().anyMatch(settings.allowedRoleIds()::contains);
    }

    private boolean cooldownElapsed(DiscordInboundMessage message) {
        Instant lastAccepted = lastAcceptedByUser.get(message.userId());
        if (lastAccepted == null || settings.cooldown().isZero()) {
            return true;
        }
        return !message.createdAt().isBefore(lastAccepted.plus(settings.cooldown()));
    }
}
