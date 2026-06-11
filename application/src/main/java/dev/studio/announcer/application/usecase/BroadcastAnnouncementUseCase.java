package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.NetworkBroadcastService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class BroadcastAnnouncementUseCase {
    private final AnnouncementRepository repository;
    private final NetworkBroadcastService networkBroadcastService;
    private final String originServer;
    private final Set<String> defaultTargetGroups;

    public BroadcastAnnouncementUseCase(
            AnnouncementRepository repository,
            NetworkBroadcastService networkBroadcastService,
            String originServer,
            Set<String> defaultTargetGroups) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.networkBroadcastService = Objects.requireNonNull(networkBroadcastService, "networkBroadcastService");
        this.originServer = originServer == null ? "" : originServer.trim();
        this.defaultTargetGroups = defaultTargetGroups == null ? Set.of() : Set.copyOf(defaultTargetGroups);
    }

    public CompletableFuture<NetworkBroadcastRequest> execute(AnnouncementId id) {
        if (!networkBroadcastService.connected()) {
            throw new IllegalStateException("Network broadcast service is not connected.");
        }
        Announcement announcement = repository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        NetworkBroadcastRequest request = requestFor(announcement);
        return networkBroadcastService.publish(request).thenApply(ignored -> request);
    }

    private NetworkBroadcastRequest requestFor(Announcement announcement) {
        Set<String> targetGroups = announcement.targetGroups().isEmpty()
                ? defaultTargetGroups
                : announcement.targetGroups();
        return new NetworkBroadcastRequest(
                "",
                originServer,
                announcement.targetServers(),
                first(targetGroups),
                targetGroups,
                announcement.permission().orElse(""),
                content(announcement),
                packetType(announcement),
                announcement.soundOptions().map(options -> options.key()).orElse(""),
                null);
    }

    private String content(Announcement announcement) {
        return announcement.messages().isEmpty() ? announcement.name() : announcement.messages().getFirst();
    }

    private String packetType(Announcement announcement) {
        List<AnnouncementChannel> priority = List.of(
                AnnouncementChannel.TOAST,
                AnnouncementChannel.BOSSBAR,
                AnnouncementChannel.ACTIONBAR,
                AnnouncementChannel.TITLE,
                AnnouncementChannel.SUBTITLE,
                AnnouncementChannel.SOUND,
                AnnouncementChannel.CHAT);
        return priority.stream()
                .filter(announcement.channels()::contains)
                .findFirst()
                .orElse(AnnouncementChannel.CHAT)
                .name();
    }

    private String first(Set<String> values) {
        return values.stream().sorted(Comparator.naturalOrder()).findFirst().orElse("");
    }
}
