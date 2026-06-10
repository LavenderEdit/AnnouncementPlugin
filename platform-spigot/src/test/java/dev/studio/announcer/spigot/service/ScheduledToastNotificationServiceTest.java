package dev.studio.announcer.spigot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.domain.announcement.option.ToastFrameType;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ScheduledToastNotificationServiceTest {

    @Test
    void showToastAddsGrantsAndRemovesVirtualToast() {
        FakeToastSender sender = new FakeToastSender();
        FakeScheduler scheduler = new FakeScheduler();
        ScheduledToastNotificationService service = new ScheduledToastNotificationService(
                sender,
                new PlainRenderer(),
                scheduler);

        service.showToast("player", enabledToast("Welcome {player_name}"), PlaceholderContext.of(Map.of(
                "player_name", "Ada")));

        assertEquals(List.of("add:Welcome Ada", "grant"), sender.events());

        scheduler.runOnlyTask();

        assertEquals(List.of("add:Welcome Ada", "grant", "remove"), sender.events());
    }

    @Test
    void disabledToastDoesNothing() {
        FakeToastSender sender = new FakeToastSender();
        ScheduledToastNotificationService service = new ScheduledToastNotificationService(
                sender,
                new PlainRenderer(),
                new FakeScheduler());
        ToastOptions options = new ToastOptions(
                false,
                "",
                "",
                "",
                ToastFrameType.TASK,
                Duration.ofSeconds(1),
                false,
                null,
                null);

        service.showToast("player", options, PlaceholderContext.empty());

        assertEquals(List.of(), sender.events());
    }

    @Test
    void closeCancelsRemovalTasksAndRemovesActiveToasts() {
        FakeToastSender sender = new FakeToastSender();
        FakeScheduler scheduler = new FakeScheduler();
        ScheduledToastNotificationService service = new ScheduledToastNotificationService(
                sender,
                new PlainRenderer(),
                scheduler);

        service.showToast("player", enabledToast("Notice"), PlaceholderContext.empty());
        service.close();

        assertTrue(scheduler.onlyTask().cancelled());
        assertEquals(List.of("add:Notice", "grant", "remove"), sender.events());
    }

    private ToastOptions enabledToast(String title) {
        return new ToastOptions(
                true,
                title,
                "Description",
                "DIAMOND",
                ToastFrameType.TASK,
                Duration.ofSeconds(2),
                false,
                null,
                null);
    }

    private static final class PlainRenderer implements MessageRenderer<Component> {

        @Override
        public ValidationResult validate(String message) {
            return ValidationResult.ok();
        }

        @Override
        public Component render(String message, PlaceholderContext context) {
            String rendered = message;
            for (Map.Entry<String, String> entry : context.values().entrySet()) {
                rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return Component.text(rendered);
        }
    }

    private static final class FakeToastSender implements ToastSender {
        private final List<String> events = new ArrayList<>();

        @Override
        public void addToast(String audienceId, VirtualToast toast) {
            events.add("add:" + ((net.kyori.adventure.text.TextComponent) toast.title()).content());
        }

        @Override
        public void grantToast(String audienceId, String toastKey) {
            events.add("grant");
        }

        @Override
        public void removeToast(String audienceId, String toastKey) {
            events.add("remove");
        }

        private List<String> events() {
            return events;
        }
    }

    private static final class FakeScheduler implements SchedulerPort {
        private final Map<String, FakeTask> tasks = new ConcurrentHashMap<>();

        @Override
        public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
            FakeTask scheduledTask = new FakeTask(taskId, task);
            tasks.put(taskId, scheduledTask);
            return scheduledTask;
        }

        @Override
        public ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
            throw new UnsupportedOperationException("Not needed by this test.");
        }

        private FakeTask onlyTask() {
            return tasks.values().iterator().next();
        }

        private void runOnlyTask() {
            onlyTask().run();
        }
    }

    private static final class FakeTask implements ScheduledTask {
        private final String id;
        private final Runnable task;
        private boolean cancelled;

        private FakeTask(String id, Runnable task) {
            this.id = id;
            this.task = task;
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
            cancelled = true;
        }

        private void run() {
            if (!cancelled) {
                task.run();
            }
        }
    }
}
