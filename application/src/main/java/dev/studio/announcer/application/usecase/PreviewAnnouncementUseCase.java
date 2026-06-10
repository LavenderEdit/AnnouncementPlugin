package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Objects;

public final class PreviewAnnouncementUseCase {
    private final AnnouncementRepository repository;
    private final AnnouncementDispatcher dispatcher;

    public PreviewAnnouncementUseCase(AnnouncementRepository repository, AnnouncementDispatcher dispatcher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    public DeliverySummary execute(AnnouncementId id, String audienceId) {
        if (audienceId == null || audienceId.isBlank()) {
            throw new IllegalArgumentException("Preview audience id cannot be blank.");
        }
        Announcement announcement = repository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        return dispatcher.preview(announcement, audienceId);
    }
}
