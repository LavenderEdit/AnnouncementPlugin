package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import dev.studio.announcer.domain.announcement.option.ActionBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarColor;
import dev.studio.announcer.domain.announcement.option.BossBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarOverlay;
import dev.studio.announcer.domain.announcement.option.SoundOptions;
import dev.studio.announcer.domain.announcement.option.TitleOptions;
import dev.studio.announcer.domain.announcement.option.ToastFrameType;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public final class HandleNetworkBroadcastUseCase {
    private final AnnouncementDispatcher dispatcher;
    private final Predicate<NetworkBroadcastRequest> filter;

    public HandleNetworkBroadcastUseCase(
            AnnouncementDispatcher dispatcher,
            Predicate<NetworkBroadcastRequest> filter) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.filter = filter == null ? request -> true : filter;
    }

    public DeliverySummary handle(NetworkBroadcastRequest request) {
        if (!filter.test(request)) {
            return DeliverySummary.empty();
        }
        return dispatcher.broadcast(toAnnouncement(request));
    }

    private Announcement toAnnouncement(NetworkBroadcastRequest request) {
        String content = request.content().isBlank() ? "Network announcement" : request.content();
        AnnouncementChannel primary = channel(request.packetType());
        EnumSet<AnnouncementChannel> channels = EnumSet.of(primary);
        if (!request.sound().isBlank()) {
            channels.add(AnnouncementChannel.SOUND);
        }
        Announcement.Builder builder = Announcement.builder(
                        AnnouncementId.of("network:" + request.messageId()),
                        "Network Broadcast")
                .enabled(true)
                .type(AnnouncementType.NETWORK)
                .channels(channels)
                .messages(List.of(content));
        if (!request.permission().isBlank()) {
            builder.permission(request.permission());
        }
        switch (primary) {
            case ACTIONBAR -> builder.actionBarOptions(new ActionBarOptions(
                    content,
                    Duration.ofSeconds(2),
                    0,
                    request.permission(),
                    Duration.ZERO));
            case BOSSBAR -> builder.bossBarOptions(new BossBarOptions(
                    content,
                    BossBarColor.WHITE,
                    BossBarOverlay.PROGRESS,
                    1.0f,
                    Duration.ofSeconds(3),
                    0,
                    request.permission(),
                    false,
                    true));
            case TOAST -> builder.toastOptions(new ToastOptions(
                    true,
                    content,
                    "",
                    "minecraft:paper",
                    ToastFrameType.TASK,
                    Duration.ofSeconds(3),
                    false,
                    null,
                    request.permission()));
            case TITLE, SUBTITLE -> builder.titleOptions(new TitleOptions(
                    primary == AnnouncementChannel.TITLE ? content : "",
                    primary == AnnouncementChannel.SUBTITLE ? content : "",
                    Duration.ofMillis(500),
                    Duration.ofSeconds(2),
                    Duration.ofMillis(500)));
            default -> {
            }
        }
        if (!request.sound().isBlank()) {
            builder.soundOptions(new SoundOptions(request.sound(), 1.0f, 1.0f));
        }
        return builder.build();
    }

    private AnnouncementChannel channel(String packetType) {
        try {
            return AnnouncementChannel.valueOf(packetType.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return AnnouncementChannel.CHAT;
        }
    }
}
