package dev.studio.announcer.spigot.service;

import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.common.notification.PriorityNotification;
import dev.studio.announcer.common.notification.PriorityNotificationDecision;
import dev.studio.announcer.common.notification.PriorityNotificationDecisionType;
import dev.studio.announcer.common.notification.PriorityNotificationManager;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;

public final class PriorityActionBarService implements AutoCloseable {
    private final ActionBarSender sender;
    private final SchedulerPort scheduler;
    private final Clock clock;
    private final PriorityNotificationManager<Component> manager = new PriorityNotificationManager<>();
    private final Map<String, ScheduledTask> activeTasks = new ConcurrentHashMap<>();
    private final Set<String> touchedAudiences = ConcurrentHashMap.newKeySet();

    public PriorityActionBarService(ActionBarSender sender, SchedulerPort scheduler) {
        this(sender, scheduler, Clock.systemUTC());
    }

    public PriorityActionBarService(ActionBarSender sender, SchedulerPort scheduler, Clock clock) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized PriorityNotificationDecision<Component> show(
            String audienceId,
            String notificationId,
            Component message,
            int priority,
            Duration duration,
            Duration antiSpamWindow) {
        PriorityNotification<Component> notification = new PriorityNotification<>(
                notificationId,
                audienceId,
                message,
                priority,
                duration,
                antiSpamWindow);
        PriorityNotificationDecision<Component> decision = manager.submit(notification, clock.instant());
        if (decision.type() == PriorityNotificationDecisionType.DISPLAY_NOW) {
            decision.notification().ifPresent(this::display);
        }
        return decision;
    }

    public synchronized void clearAudience(String audienceId) {
        ScheduledTask activeTask = activeTasks.remove(audienceId);
        if (activeTask != null) {
            activeTask.close();
        }
        manager.clearAudience(audienceId);
        touchedAudiences.remove(audienceId);
        sender.clear(audienceId);
    }

    @Override
    public synchronized void close() {
        activeTasks.values().forEach(ScheduledTask::close);
        activeTasks.clear();
        manager.clearAll();
        touchedAudiences.forEach(sender::clear);
        touchedAudiences.clear();
    }

    private synchronized void display(PriorityNotification<Component> notification) {
        String audienceId = notification.audienceId();
        ScheduledTask previousTask = activeTasks.remove(audienceId);
        if (previousTask != null) {
            previousTask.close();
        }
        touchedAudiences.add(audienceId);
        sender.show(audienceId, notification.payload());
        ScheduledTask task = scheduler.scheduleOnce(
                taskId(audienceId, notification.id()),
                notification.duration(),
                () -> complete(audienceId, notification.id()));
        activeTasks.put(audienceId, task);
    }

    private synchronized void complete(String audienceId, String notificationId) {
        activeTasks.remove(audienceId);
        manager.complete(audienceId, notificationId, clock.instant())
                .ifPresentOrElse(this::display, () -> sender.clear(audienceId));
    }

    private String taskId(String audienceId, String notificationId) {
        return "actionbar-" + safeTaskPart(audienceId) + "-" + safeTaskPart(notificationId);
    }

    private String safeTaskPart(String value) {
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
