package dev.studio.announcer.common.service;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.service.ToastNotificationService;
import dev.studio.announcer.domain.announcement.option.ToastOptions;

public final class NoopToastNotificationService implements ToastNotificationService {

    @Override
    public void showToast(String audienceId, ToastOptions options, PlaceholderContext context) {
        // Intentionally empty until a packet-backed toast adapter is introduced in a later phase.
    }
}
