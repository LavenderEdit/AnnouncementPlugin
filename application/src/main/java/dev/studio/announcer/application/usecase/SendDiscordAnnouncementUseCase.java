package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DiscordBridgeService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class SendDiscordAnnouncementUseCase {
    private final AnnouncementRepository repository;
    private final DiscordBridgeService discordBridgeService;

    public SendDiscordAnnouncementUseCase(
            AnnouncementRepository repository,
            DiscordBridgeService discordBridgeService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.discordBridgeService = Objects.requireNonNull(discordBridgeService, "discordBridgeService");
    }

    public CompletableFuture<DiscordOutboundMessage> execute(AnnouncementId id) {
        if (!discordBridgeService.enabled()) {
            throw new IllegalStateException("Discord bridge is not enabled.");
        }
        Announcement announcement = repository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        DiscordOutboundMessage message = new DiscordOutboundMessage(
                announcement.name(),
                announcement.messages().isEmpty() ? announcement.name() : announcement.messages().getFirst());
        return discordBridgeService.send(message).thenApply(ignored -> message);
    }
}
