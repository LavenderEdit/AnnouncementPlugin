package dev.studio.announcer.spigot.scheduler;

import dev.studio.announcer.api.service.ScheduledTask;
import java.util.Objects;

public final class ScheduledPlatformTask implements ScheduledTask {
    private final String id;
    private final Runnable cancelAction;
    private volatile boolean cancelled;

    public ScheduledPlatformTask(String id, Runnable cancelAction) {
        this.id = Objects.requireNonNull(id, "id");
        this.cancelAction = cancelAction == null ? () -> {
        } : cancelAction;
    }

    public static ScheduledPlatformTask noop(String id) {
        return new ScheduledPlatformTask(id, null);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void close() {
        if (!cancelled) {
            cancelled = true;
            cancelAction.run();
        }
    }
}
