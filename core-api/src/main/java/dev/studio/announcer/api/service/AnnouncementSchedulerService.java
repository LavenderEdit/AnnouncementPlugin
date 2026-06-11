package dev.studio.announcer.api.service;

import dev.studio.announcer.domain.announcement.Announcement;
import java.util.Collection;

public interface AnnouncementSchedulerService extends AutoCloseable {

    void reschedule(Collection<Announcement> announcements);

    void stop();

    @Override
    default void close() {
        stop();
    }
}
