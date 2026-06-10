package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Objects;

public final class SendAnnouncementUseCase {
    private final AnnouncementRepository repository;
    private final AnnouncementDispatcher dispatcher;

    public SendAnnouncementUseCase(AnnouncementRepository repository, AnnouncementDispatcher dispatcher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    public DeliverySummary execute(AnnouncementId id) {
        Announcement announcement = repository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        return dispatcher.broadcast(announcement);
    }
}
