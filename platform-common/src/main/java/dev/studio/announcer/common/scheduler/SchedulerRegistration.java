package dev.studio.announcer.common.scheduler;

import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Objects;

record SchedulerRegistration(AnnouncementId announcementId, ScheduledTask task) {

    SchedulerRegistration {
        announcementId = Objects.requireNonNull(announcementId, "announcementId");
        task = Objects.requireNonNull(task, "task");
    }

    void cancel() {
        task.close();
    }
}
