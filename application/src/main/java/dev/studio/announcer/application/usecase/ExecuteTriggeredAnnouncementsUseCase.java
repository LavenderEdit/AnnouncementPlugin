package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementExecutionContext;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ExecuteTriggeredAnnouncementsUseCase {
    private static final Logger LOGGER = Logger.getLogger(ExecuteTriggeredAnnouncementsUseCase.class.getName());

    private final AnnouncementRepository repository;
    private final AnnouncementDispatcher dispatcher;
    private final SchedulerPort scheduler;
    private final TriggerMatcher matcher;
    private final Duration defaultJoinDelay;

    public ExecuteTriggeredAnnouncementsUseCase(
            AnnouncementRepository repository,
            AnnouncementDispatcher dispatcher,
            SchedulerPort scheduler,
            TriggerMatcher matcher,
            Duration defaultJoinDelay) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.matcher = Objects.requireNonNull(matcher, "matcher");
        this.defaultJoinDelay = defaultJoinDelay == null ? Duration.ZERO : defaultJoinDelay;
    }

    public void execute(AnnouncementType type, AnnouncementExecutionContext context) {
        if (context == null || context.actorId() == null || context.actorId().isBlank()) {
            return;
        }

        List<Announcement> announcements = repository.findAll().stream()
                .filter(announcement -> matcher.matches(announcement, type))
                .toList();

        for (Announcement announcement : announcements) {
            Duration delay = defaultJoinDelay;
            String overrideDelay = announcement.metadata().get("join-delay");
            if (overrideDelay != null && !overrideDelay.isBlank()) {
                try {
                    delay = Duration.parse(overrideDelay);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Invalid join-delay metadata format for announcement " 
                            + announcement.id().value() + ": " + overrideDelay, e);
                }
            }

            String taskId = "triggered-announcement-" + announcement.id().value() + "-" + context.actorId();
            scheduler.scheduleOnce(taskId, delay, () -> {
                try {
                    dispatcher.dispatch(announcement, context.actorId());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error delivering triggered announcement " 
                            + announcement.id().value() + " to actor " + context.actorId(), e);
                }
            });
        }
    }
}
