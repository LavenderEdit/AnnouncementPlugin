package dev.studio.announcer.application.usecase;

import dev.studio.announcer.domain.validation.ValidationResult;

public final class AnnouncementValidationException extends RuntimeException {
    private final ValidationResult result;

    public AnnouncementValidationException(ValidationResult result) {
        super(String.join("; ", result.errors()));
        this.result = result;
    }

    public ValidationResult result() {
        return result;
    }
}
