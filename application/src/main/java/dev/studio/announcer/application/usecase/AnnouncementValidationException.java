package dev.studio.announcer.application.usecase;

import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.List;

public final class AnnouncementValidationException extends RuntimeException {
    private final ValidationResult result;

    public AnnouncementValidationException(ValidationResult result) {
        super(String.join("; ", result != null ? result.errors() : List.of("Unknown validation error")));
        this.result = result;
    }

    public ValidationResult result() {
        return result;
    }
}
