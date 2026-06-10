package dev.studio.announcer.application.usecase;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementValidator;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.Objects;

public final class ValidateAnnouncementUseCase {
    private final AnnouncementValidator validator;

    public ValidateAnnouncementUseCase() {
        this(new AnnouncementValidator());
    }

    public ValidateAnnouncementUseCase(AnnouncementValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public ValidationResult execute(Announcement announcement) {
        return validator.validate(announcement);
    }
}
