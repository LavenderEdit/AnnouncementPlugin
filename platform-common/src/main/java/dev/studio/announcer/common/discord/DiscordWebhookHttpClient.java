package dev.studio.announcer.common.discord;

import java.net.http.HttpRequest;
import java.util.concurrent.CompletableFuture;

public interface DiscordWebhookHttpClient {

    CompletableFuture<Integer> send(HttpRequest request);
}
