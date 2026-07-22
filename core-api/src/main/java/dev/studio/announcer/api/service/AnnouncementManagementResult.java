package dev.studio.announcer.api.service;

import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.List;
import java.util.Objects;

public record AnnouncementManagementResult(
        boolean success,
        String message,
        List<String> errors,
        ValidationResult validationResult) {

    public AnnouncementManagementResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static AnnouncementManagementResult success(String message) {
        return new AnnouncementManagementResult(true, message, List.of(), ValidationResult.ok());
    }

    public static AnnouncementManagementResult failure(String message) {
        return new AnnouncementManagementResult(false, message, List.of(message), ValidationResult.invalid(message));
    }

    public static AnnouncementManagementResult failure(String message, List<String> errors) {
        return new AnnouncementManagementResult(false, message, errors, ValidationResult.fromErrors(errors));
    }

    public static AnnouncementManagementResult validationFailure(ValidationResult result) {
        List<String> errors = result != null ? result.errors() : List.of("Unknown validation error");
        return new AnnouncementManagementResult(false, "Validation failed", errors, result);
    }
}
