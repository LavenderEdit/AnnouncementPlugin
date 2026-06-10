package dev.studio.announcer.spigot.service;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.api.service.ToastNotificationService;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;

public final class ScheduledToastNotificationService implements ToastNotificationService, AutoCloseable {
    private final ToastSender sender;
    private final MessageRenderer<Component> renderer;
    private final SchedulerPort scheduler;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, ActiveToast> activeToasts = new ConcurrentHashMap<>();

    public ScheduledToastNotificationService(
            ToastSender sender,
            MessageRenderer<Component> renderer,
            SchedulerPort scheduler) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void showToast(String audienceId, ToastOptions options, PlaceholderContext context) {
        if (options == null || !options.enabled()) {
            return;
        }
        PlaceholderContext safeContext = context == null ? PlaceholderContext.empty() : context;
        String toastKey = nextToastKey(audienceId);
        VirtualToast toast = new VirtualToast(
                toastKey,
                renderer.render(options.title(), safeContext),
                renderer.render(options.description(), safeContext),
                options.iconMaterial(),
                options.frameType(),
                options.dynamicPlayerHeadIcon(),
                options.customModelData());

        sender.addToast(audienceId, toast);
        sender.grantToast(audienceId, toastKey);
        ScheduledTask removalTask = scheduler.scheduleOnce(
                "toast-remove-" + safeTaskPart(audienceId) + "-" + sequence.get(),
                options.duration(),
                () -> removeToast(toastKey));
        activeToasts.put(toastKey, new ActiveToast(audienceId, toastKey, removalTask));
    }

    @Override
    public void close() {
        activeToasts.values().forEach(active -> {
            active.removalTask().close();
            sender.removeToast(active.audienceId(), active.toastKey());
        });
        activeToasts.clear();
    }

    private void removeToast(String toastKey) {
        ActiveToast active = activeToasts.remove(toastKey);
        if (active != null) {
            sender.removeToast(active.audienceId(), active.toastKey());
        }
    }

    private String nextToastKey(String audienceId) {
        return "advancedannouncer:toast/" + safeTaskPart(audienceId).toLowerCase(java.util.Locale.ROOT)
                + "/" + sequence.incrementAndGet();
    }

    private String safeTaskPart(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private record ActiveToast(String audienceId, String toastKey, ScheduledTask removalTask) {
    }
}
