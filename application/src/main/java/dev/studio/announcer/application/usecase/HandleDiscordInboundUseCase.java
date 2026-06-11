package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class HandleDiscordInboundUseCase {
    private final AnnouncementDispatcher dispatcher;
    private final String format;

    public HandleDiscordInboundUseCase(AnnouncementDispatcher dispatcher, String format) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.format = format == null || format.isBlank()
                ? "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>"
                : format;
    }

    public DeliverySummary handle(DiscordInboundMessage message) {
        String rendered = format
                .replace("%discord_user%", message.username())
                .replace("%discord_message%", message.content())
                .replace("%discord_channel%", message.channelId());
        Announcement announcement = Announcement.builder(
                        AnnouncementId.of("discord:" + message.userId() + ":" + message.createdAt().toEpochMilli()),
                        "Discord Message")
                .type(AnnouncementType.DISCORD)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of(rendered))
                .build();
        return dispatcher.broadcast(announcement);
    }
}
