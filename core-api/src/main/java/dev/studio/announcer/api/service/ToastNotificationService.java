package dev.studio.announcer.api.service;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.domain.announcement.option.ToastOptions;

public interface ToastNotificationService {

    void showToast(String audienceId, ToastOptions options, PlaceholderContext context);
}
