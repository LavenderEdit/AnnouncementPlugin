package dev.studio.announcer.common.discord;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import dev.studio.announcer.api.service.DiscordBridgeService;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class DiscordWebhookBridgeService implements DiscordBridgeService {
    private final DiscordWebhookHttpClient httpClient;
    private final DiscordWebhookPayloadFactory payloadFactory;
    private final DiscordWebhookSettings settings;

    public DiscordWebhookBridgeService(DiscordWebhookSettings settings) {
        this(new JavaDiscordWebhookHttpClient(), new DiscordWebhookPayloadFactory(), settings);
    }

    public DiscordWebhookBridgeService(
            DiscordWebhookHttpClient httpClient,
            DiscordWebhookPayloadFactory payloadFactory,
            DiscordWebhookSettings settings) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.payloadFactory = Objects.requireNonNull(payloadFactory, "payloadFactory");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public CompletableFuture<Void> send(DiscordOutboundMessage message) {
        if (!enabled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Discord webhook is not configured."));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.webhookUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadFactory.payload(message, settings)))
                .build();
        return httpClient.send(request).thenCompose(status -> {
            if (status >= 200 && status < 300) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Discord webhook failed with status " + status + "."));
        });
    }

    @Override
    public void setInboundHandler(Consumer<DiscordInboundMessage> handler) {
        // Webhooks are outbound-only. DiscordSRV supplies inbound messages.
    }

    @Override
    public boolean enabled() {
        return settings.enabled();
    }

    @Override
    public void close() {
    }
}
