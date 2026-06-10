package dev.studio.announcer.application.usecase;

import dev.studio.announcer.domain.announcement.AnnouncementId;

public final class AnnouncementNotFoundException extends RuntimeException {

    public AnnouncementNotFoundException(AnnouncementId id) {
        super("Announcement not found: " + id.value());
    }
}
