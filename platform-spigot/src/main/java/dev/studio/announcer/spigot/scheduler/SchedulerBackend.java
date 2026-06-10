package dev.studio.announcer.spigot.scheduler;

import java.time.Duration;

interface SchedulerBackend {

    boolean foliaAvailable();

    ScheduledPlatformTask scheduleFolia(String taskId, Duration delay, Runnable task);

    default ScheduledPlatformTask scheduleFoliaRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
        return scheduleFolia(taskId, initialDelay, task);
    }

    ScheduledPlatformTask scheduleBukkit(String taskId, Duration delay, Runnable task);

    ScheduledPlatformTask scheduleBukkitRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task);
}
