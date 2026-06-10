package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementValidator;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.Objects;

public final class CreateAnnouncementUseCase {
    private final AnnouncementRepository repository;
    private final AnnouncementValidator validator;

    public CreateAnnouncementUseCase(AnnouncementRepository repository) {
        this(repository, new AnnouncementValidator());
    }

    public CreateAnnouncementUseCase(AnnouncementRepository repository, AnnouncementValidator validator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public Announcement execute(Announcement announcement) {
        ValidationResult result = validator.validate(announcement);
        if (!result.valid()) {
            throw new AnnouncementValidationException(result);
        }
        return repository.save(announcement);
    }
}
