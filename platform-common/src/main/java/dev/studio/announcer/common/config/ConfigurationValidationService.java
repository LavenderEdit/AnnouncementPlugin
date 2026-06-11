package dev.studio.announcer.common.config;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementValidator;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class ConfigurationValidationService {
    private final AnnouncementValidator announcementValidator = new AnnouncementValidator();
    private final MessageRenderer<?> renderer;

    public ConfigurationValidationService(MessageRenderer<?> renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public ValidationResult validateAnnouncements(Collection<Announcement> announcements) {
        List<String> errors = new ArrayList<>();
        if (announcements == null) {
            return ValidationResult.invalid("Announcements cannot be null.");
        }
        for (Announcement announcement : announcements) {
            ValidationResult domain = announcementValidator.validate(announcement);
            domain.errors().forEach(error -> errors.add(announcement.id().value() + ": " + error));
            for (String message : announcement.messages()) {
                ValidationResult rendered = renderer.validate(message);
                rendered.errors().forEach(error -> errors.add(announcement.id().value() + ": " + error));
            }
            announcement.titleOptions().ifPresent(options -> {
                collect(errors, announcement, renderer.validate(options.title()));
                collect(errors, announcement, renderer.validate(options.subtitle()));
            });
            announcement.actionBarOptions().ifPresent(options -> collect(errors, announcement, renderer.validate(options.message())));
            announcement.bossBarOptions().ifPresent(options -> collect(errors, announcement, renderer.validate(options.title())));
            announcement.toastOptions().ifPresent(options -> {
                collect(errors, announcement, renderer.validate(options.title()));
                collect(errors, announcement, renderer.validate(options.description()));
            });
            announcement.cronExpression().ifPresent(cron -> {
                String[] fields = cron.trim().split("\\s+");
                if (fields.length < 5 || fields.length > 6) {
                    errors.add(announcement.id().value() + ": cron expression must contain 5 or 6 fields.");
                }
            });
        }
        return ValidationResult.fromErrors(errors);
    }

    private void collect(List<String> errors, Announcement announcement, ValidationResult result) {
        result.errors().forEach(error -> errors.add(announcement.id().value() + ": " + error));
    }
}
