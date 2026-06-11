package dev.studio.announcer.common.scheduler;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultAnnouncementSchedulerService implements AnnouncementSchedulerService {
    private final SchedulerPort scheduler;
    private final AnnouncementDispatcher dispatcher;
    private final Map<AnnouncementId, SchedulerRegistration> registrations = new ConcurrentHashMap<>();

    public DefaultAnnouncementSchedulerService(SchedulerPort scheduler, AnnouncementDispatcher dispatcher) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    @Override
    public synchronized void reschedule(Collection<Announcement> announcements) {
        stop();
        if (announcements == null) {
            return;
        }
        announcements.stream()
                .filter(Announcement::enabled)
                .filter(announcement -> announcement.interval().isPresent())
                .forEach(this::schedule);
    }

    @Override
    public synchronized void stop() {
        registrations.values().forEach(SchedulerRegistration::cancel);
        registrations.clear();
    }

    private void schedule(Announcement announcement) {
        Duration interval = announcement.interval().orElseThrow();
        ScheduledTask task = scheduler.scheduleRepeating(
                taskId(announcement),
                interval,
                interval,
                () -> dispatcher.broadcast(announcement));
        registrations.put(announcement.id(), new SchedulerRegistration(announcement.id(), task));
    }

    private String taskId(Announcement announcement) {
        return "announcement-" + announcement.id().value().replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
