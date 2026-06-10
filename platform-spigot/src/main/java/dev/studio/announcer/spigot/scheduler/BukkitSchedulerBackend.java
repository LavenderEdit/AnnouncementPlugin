package dev.studio.announcer.spigot.scheduler;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

final class BukkitSchedulerBackend implements SchedulerBackend {
    private final JavaPlugin plugin;
    private final boolean foliaAvailable;

    BukkitSchedulerBackend(JavaPlugin plugin) {
        this.plugin = plugin;
        this.foliaAvailable = detectFolia();
    }

    @Override
    public boolean foliaAvailable() {
        return foliaAvailable;
    }

    @Override
    public ScheduledPlatformTask scheduleFolia(String taskId, Duration delay, Runnable task) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method runDelayed = scheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, long.class);
            Object scheduled = runDelayed.invoke(scheduler, plugin, (Consumer<Object>) ignored -> task.run(), ticks(delay));
            return reflectiveTask(taskId, scheduled);
        } catch (ReflectiveOperationException ex) {
            return scheduleBukkit(taskId, delay, task);
        }
    }

    @Override
    public ScheduledPlatformTask scheduleFoliaRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method runAtFixedRate = scheduler.getClass().getMethod(
                    "runAtFixedRate",
                    org.bukkit.plugin.Plugin.class,
                    Consumer.class,
                    long.class,
                    long.class);
            Object scheduled = runAtFixedRate.invoke(
                    scheduler,
                    plugin,
                    (Consumer<Object>) ignored -> task.run(),
                    ticks(initialDelay),
                    Math.max(1L, ticks(interval)));
            return reflectiveTask(taskId, scheduled);
        } catch (ReflectiveOperationException ex) {
            return scheduleBukkitRepeating(taskId, initialDelay, interval, task);
        }
    }

    @Override
    public ScheduledPlatformTask scheduleBukkit(String taskId, Duration delay, Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, ticks(delay));
        return new ScheduledPlatformTask(taskId, bukkitTask::cancel);
    }

    @Override
    public ScheduledPlatformTask scheduleBukkitRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, ticks(initialDelay), Math.max(1L, ticks(interval)));
        return new ScheduledPlatformTask(taskId, bukkitTask::cancel);
    }

    private boolean detectFolia() {
        try {
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private ScheduledPlatformTask reflectiveTask(String taskId, Object scheduledTask) {
        return new ScheduledPlatformTask(taskId, () -> {
            if (scheduledTask == null) {
                return;
            }
            try {
                scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
            } catch (ReflectiveOperationException ignored) {
                // Scheduler task implementation does not expose cancellation on this platform.
            }
        });
    }

    private long ticks(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return 0L;
        }
        long ticks = duration.toMillis() / 50L;
        return Math.max(1L, ticks);
    }
}
