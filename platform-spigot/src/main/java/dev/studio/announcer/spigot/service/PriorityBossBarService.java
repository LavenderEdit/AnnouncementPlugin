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
import net.kyori.adventure.bossbar.BossBar;

public final class PriorityBossBarService implements AutoCloseable {
    private static final Duration PROGRESS_INTERVAL = Duration.ofMillis(250);

    private final BossBarSender sender;
    private final SchedulerPort scheduler;
    private final Clock clock;
    private final PriorityNotificationManager<BossBarPayload> manager = new PriorityNotificationManager<>();
    private final Map<String, ScheduledTask> finishTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledTask> progressTasks = new ConcurrentHashMap<>();
    private final Map<String, BossBar> visibleBars = new ConcurrentHashMap<>();
    private final Set<String> touchedAudiences = ConcurrentHashMap.newKeySet();

    public PriorityBossBarService(BossBarSender sender, SchedulerPort scheduler) {
        this(sender, scheduler, Clock.systemUTC());
    }

    public PriorityBossBarService(BossBarSender sender, SchedulerPort scheduler, Clock clock) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized PriorityNotificationDecision<BossBarPayload> show(
            String audienceId,
            String notificationId,
            BossBar bossBar,
            int priority,
            Duration duration,
            boolean animatedProgress,
            boolean autoHide) {
        PriorityNotification<BossBarPayload> notification = new PriorityNotification<>(
                notificationId,
                audienceId,
                new BossBarPayload(bossBar, animatedProgress, autoHide, clock.instant(), duration),
                priority,
                duration,
                Duration.ZERO);
        PriorityNotificationDecision<BossBarPayload> decision = manager.submit(notification, clock.instant());
        if (decision.type() == PriorityNotificationDecisionType.DISPLAY_NOW) {
            decision.notification().ifPresent(this::display);
        }
        return decision;
    }

    public synchronized void clearAudience(String audienceId) {
        cancel(finishTasks.remove(audienceId));
        cancel(progressTasks.remove(audienceId));
        BossBar visible = visibleBars.remove(audienceId);
        if (visible != null) {
            sender.hide(audienceId, visible);
        }
        manager.clearAudience(audienceId);
        touchedAudiences.remove(audienceId);
    }

    @Override
    public synchronized void close() {
        finishTasks.values().forEach(ScheduledTask::close);
        progressTasks.values().forEach(ScheduledTask::close);
        finishTasks.clear();
        progressTasks.clear();
        visibleBars.forEach(sender::hide);
        visibleBars.clear();
        touchedAudiences.clear();
        manager.clearAll();
    }

    private synchronized void display(PriorityNotification<BossBarPayload> notification) {
        String audienceId = notification.audienceId();
        cancel(finishTasks.remove(audienceId));
        cancel(progressTasks.remove(audienceId));

        BossBar previous = visibleBars.remove(audienceId);
        if (previous != null) {
            sender.hide(audienceId, previous);
        }

        BossBarPayload payload = notification.payload();
        touchedAudiences.add(audienceId);
        visibleBars.put(audienceId, payload.bossBar());
        sender.show(audienceId, payload.bossBar());

        if (payload.animatedProgress()) {
            ScheduledTask progressTask = scheduler.scheduleRepeating(
                    progressTaskId(audienceId, notification.id()),
                    PROGRESS_INTERVAL,
                    PROGRESS_INTERVAL,
                    () -> animate(notification));
            progressTasks.put(audienceId, progressTask);
        }

        ScheduledTask finishTask = scheduler.scheduleOnce(
                finishTaskId(audienceId, notification.id()),
                notification.duration(),
                () -> complete(audienceId, notification.id(), payload));
        finishTasks.put(audienceId, finishTask);
    }

    private synchronized void animate(PriorityNotification<BossBarPayload> notification) {
        BossBarPayload payload = notification.payload();
        Duration elapsed = Duration.between(payload.startedAt(), clock.instant());
        float remaining = 1.0f - Math.min(1.0f, (float) elapsed.toMillis() / (float) payload.totalDuration().toMillis());
        payload.bossBar().progress(Math.max(0.0f, remaining));
    }

    private synchronized void complete(String audienceId, String notificationId, BossBarPayload completedPayload) {
        cancel(finishTasks.remove(audienceId));
        cancel(progressTasks.remove(audienceId));
        java.util.Optional<PriorityNotification<BossBarPayload>> next = manager.complete(
                audienceId,
                notificationId,
                clock.instant());
        if (completedPayload.autoHide() || next.isPresent()) {
            BossBar visible = visibleBars.remove(audienceId);
            if (visible != null) {
                sender.hide(audienceId, visible);
            }
        }
        next.ifPresent(this::display);
    }

    private void cancel(ScheduledTask task) {
        if (task != null) {
            task.close();
        }
    }

    private String finishTaskId(String audienceId, String notificationId) {
        return "bossbar-finish-" + safeTaskPart(audienceId) + "-" + safeTaskPart(notificationId);
    }

    private String progressTaskId(String audienceId, String notificationId) {
        return "bossbar-progress-" + safeTaskPart(audienceId) + "-" + safeTaskPart(notificationId);
    }

    private String safeTaskPart(String value) {
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    public record BossBarPayload(
            BossBar bossBar,
            boolean animatedProgress,
            boolean autoHide,
            java.time.Instant startedAt,
            Duration totalDuration) {

        public BossBarPayload {
            bossBar = Objects.requireNonNull(bossBar, "bossBar");
            startedAt = Objects.requireNonNull(startedAt, "startedAt");
            totalDuration = Objects.requireNonNull(totalDuration, "totalDuration");
        }
    }
}
