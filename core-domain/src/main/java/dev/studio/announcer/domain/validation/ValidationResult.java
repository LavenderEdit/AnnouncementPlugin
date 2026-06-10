package dev.studio.announcer.domain.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record ValidationResult(List<String> errors) {

    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ValidationResult ok() {
        return new ValidationResult(List.of());
    }

    public static ValidationResult invalid(String error) {
        if (error == null || error.isBlank()) {
            return new ValidationResult(List.of("Validation failed."));
        }
        return new ValidationResult(List.of(error));
    }

    public static ValidationResult fromErrors(Collection<String> errors) {
        return new ValidationResult(errors == null ? List.of() : List.copyOf(errors));
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public ValidationResult merge(ValidationResult other) {
        if (other == null || other.valid()) {
            return this;
        }
        if (valid()) {
            return other;
        }
        List<String> merged = new ArrayList<>(errors);
        merged.addAll(other.errors());
        return new ValidationResult(merged);
    }
}
