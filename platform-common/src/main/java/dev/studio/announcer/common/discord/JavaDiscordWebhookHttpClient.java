package dev.studio.announcer.common.discord;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public final class JavaDiscordWebhookHttpClient implements DiscordWebhookHttpClient {
    private final HttpClient client;

    public JavaDiscordWebhookHttpClient() {
        this(HttpClient.newHttpClient());
    }

    JavaDiscordWebhookHttpClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<Integer> send(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(HttpResponse::statusCode);
    }
}
