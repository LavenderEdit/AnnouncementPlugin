package dev.studio.announcer.spigot.scheduler;

import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import java.time.Duration;
import java.util.Objects;

public class PlatformSchedulerAdapter implements SchedulerPort {
    private final SchedulerBackend backend;

    public PlatformSchedulerAdapter(SchedulerBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    @Override
    public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
        Duration safeDelay = normalize(delay);
        if (backend.foliaAvailable()) {
            return backend.scheduleFolia(taskId, safeDelay, task);
        }
        return backend.scheduleBukkit(taskId, safeDelay, task);
    }

    @Override
    public ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
        Duration safeInitialDelay = normalize(initialDelay);
        Duration safeInterval = normalize(interval);
        if (backend.foliaAvailable()) {
            return backend.scheduleFoliaRepeating(taskId, safeInitialDelay, safeInterval, task);
        }
        return backend.scheduleBukkitRepeating(taskId, safeInitialDelay, safeInterval, task);
    }

    private Duration normalize(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }
}
