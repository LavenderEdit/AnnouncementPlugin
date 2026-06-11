package dev.studio.announcer.common.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class DiscordWebhookBridgeServiceTest {

    @Test
    void sendsAsyncWebhookRequestWhenEnabled() {
        FakeDiscordWebhookHttpClient client = new FakeDiscordWebhookHttpClient(204);
        DiscordWebhookBridgeService service = new DiscordWebhookBridgeService(
                client,
                new DiscordWebhookPayloadFactory(),
                new DiscordWebhookSettings("https://discord.test/webhook", "AA", 0xAA00AA, "", "", true));

        service.send(new DiscordOutboundMessage("Title", "Description")).join();

        assertEquals(URI.create("https://discord.test/webhook"), client.requests.getFirst().uri());
    }

    @Test
    void failsForNonSuccessWebhookStatus() {
        FakeDiscordWebhookHttpClient client = new FakeDiscordWebhookHttpClient(500);
        DiscordWebhookBridgeService service = new DiscordWebhookBridgeService(
                client,
                new DiscordWebhookPayloadFactory(),
                new DiscordWebhookSettings("https://discord.test/webhook", "AA", 0xAA00AA, "", "", true));

        assertThrows(java.util.concurrent.CompletionException.class, () ->
                service.send(new DiscordOutboundMessage("Title", "Description")).join());
    }

    @Test
    void blankWebhookUrlDisablesService() {
        DiscordWebhookBridgeService service = new DiscordWebhookBridgeService(
                new FakeDiscordWebhookHttpClient(204),
                new DiscordWebhookPayloadFactory(),
                new DiscordWebhookSettings("", "AA", 0xAA00AA, "", "", true));

        assertFalse(service.enabled());
    }

    private static final class FakeDiscordWebhookHttpClient implements DiscordWebhookHttpClient {
        private final int statusCode;
        private final List<HttpRequest> requests = new ArrayList<>();

        private FakeDiscordWebhookHttpClient(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public CompletableFuture<Integer> send(HttpRequest request) {
            requests.add(request);
            return CompletableFuture.completedFuture(statusCode);
        }
    }
}
