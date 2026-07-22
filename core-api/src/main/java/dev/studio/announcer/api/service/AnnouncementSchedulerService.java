package dev.studio.announcer.api.service;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Collection;

public interface AnnouncementSchedulerService extends AutoCloseable {

    void reschedule(Collection<Announcement> announcements);

    void schedule(Announcement announcement);

    void cancel(AnnouncementId announcementId);

    boolean isScheduled(AnnouncementId announcementId);

    void stop();

    @Override
    default void close() {
        stop();
    }
}
