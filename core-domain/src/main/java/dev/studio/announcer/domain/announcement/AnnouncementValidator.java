package dev.studio.announcer.domain.announcement;

import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.ArrayList;
import java.util.List;

public final class AnnouncementValidator {

    public ValidationResult validate(Announcement announcement) {
        List<String> errors = new ArrayList<>();
        if (announcement == null) {
            errors.add("Announcement cannot be null.");
            return ValidationResult.fromErrors(errors);
        }
        if (announcement.channels().isEmpty()) {
            errors.add("Announcement requires at least one channel.");
        }
        boolean hasTextChannel = announcement.channels().stream().anyMatch(channel -> switch (channel) {
            case CHAT, TITLE, SUBTITLE, ACTIONBAR, BOSSBAR, TOAST, DISCORD_WEBHOOK -> true;
            case SOUND -> false;
        });
        if (hasTextChannel && announcement.messages().isEmpty()
                && announcement.titleOptions().isEmpty()
                && announcement.actionBarOptions().isEmpty()
                && announcement.bossBarOptions().isEmpty()
                && announcement.toastOptions().isEmpty()) {
            errors.add("Announcement text channels require messages or channel-specific options.");
        }
        return ValidationResult.fromErrors(errors);
    }
}
