package dev.studio.announcer.spigot.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class PlatformSchedulerAdapterTest {

    @Test
    void usesFoliaBackendWhenFoliaIsAvailable() {
        FakeBackend backend = new FakeBackend(true);
        PlatformSchedulerAdapter scheduler = new PlatformSchedulerAdapter(backend);

        scheduler.scheduleOnce("task", Duration.ofMillis(50), () -> {
        });

        assertEquals("folia:task:50", backend.calls);
    }

    @Test
    void fallsBackToBukkitBackendWhenFoliaIsNotAvailable() {
        FakeBackend backend = new FakeBackend(false);
        PlatformSchedulerAdapter scheduler = new PlatformSchedulerAdapter(backend);

        scheduler.scheduleRepeating("loop", Duration.ofMillis(10), Duration.ofMillis(250), () -> {
        });

        assertEquals("bukkit-repeating:loop:10:250", backend.calls);
    }

    @Test
    void returnedTaskCanBeCancelled() {
        FakeBackend backend = new FakeBackend(false);

        ScheduledPlatformTask task = (ScheduledPlatformTask) new PlatformSchedulerAdapter(backend)
                .scheduleOnce("task", Duration.ZERO, () -> {
                });
        task.close();

        assertTrue(task.cancelled());
    }

    private static final class FakeBackend implements SchedulerBackend {
        private final boolean folia;
        private String calls = "";

        private FakeBackend(boolean folia) {
            this.folia = folia;
        }

        @Override
        public boolean foliaAvailable() {
            return folia;
        }

        @Override
        public ScheduledPlatformTask scheduleFolia(String taskId, Duration delay, Runnable task) {
            calls = "folia:" + taskId + ":" + delay.toMillis();
            return ScheduledPlatformTask.noop(taskId);
        }

        @Override
        public ScheduledPlatformTask scheduleBukkit(String taskId, Duration delay, Runnable task) {
            calls = "bukkit:" + taskId + ":" + delay.toMillis();
            return ScheduledPlatformTask.noop(taskId);
        }

        @Override
        public ScheduledPlatformTask scheduleBukkitRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
            calls = "bukkit-repeating:" + taskId + ":" + initialDelay.toMillis() + ":" + interval.toMillis();
            return ScheduledPlatformTask.noop(taskId);
        }
    }
}
