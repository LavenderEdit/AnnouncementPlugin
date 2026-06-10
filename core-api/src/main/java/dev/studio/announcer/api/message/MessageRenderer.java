package dev.studio.announcer.api.message;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.domain.validation.ValidationResult;

public interface MessageRenderer<T> {

    ValidationResult validate(String input);

    T render(String input, PlaceholderContext context);
}
