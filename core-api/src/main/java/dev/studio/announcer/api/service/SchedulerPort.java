package dev.studio.announcer.api.service;

import java.time.Duration;

public interface SchedulerPort {

    ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task);

    ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task);
}
