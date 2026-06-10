package dev.studio.announcer.common.message;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.placeholder.PlaceholderResolver;
import dev.studio.announcer.domain.validation.ValidationResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class MiniMessageComponentRenderer implements MessageRenderer<Component> {
    private final MiniMessage miniMessage;
    private final PlaceholderResolver placeholderResolver;

    public MiniMessageComponentRenderer() {
        this(new InternalPlaceholderResolver());
    }

    public MiniMessageComponentRenderer(PlaceholderResolver placeholderResolver) {
        this.miniMessage = MiniMessage.builder().strict(true).build();
        this.placeholderResolver = placeholderResolver == null
                ? new InternalPlaceholderResolver()
                : placeholderResolver;
    }

    @Override
    public ValidationResult validate(String input) {
        ValidationResult clickValidation = validateClickCommand(input);
        if (!clickValidation.valid()) {
            return clickValidation;
        }
        try {
            miniMessage.deserialize(input == null ? "" : input);
            return ValidationResult.ok();
        } catch (RuntimeException ex) {
            return ValidationResult.invalid(ex.getMessage());
        }
    }

    @Override
    public Component render(String input, PlaceholderContext context) {
        String resolved = placeholderResolver.resolve(input, context == null ? PlaceholderContext.empty() : context);
        ValidationResult result = validate(resolved);
        if (!result.valid()) {
            throw new IllegalArgumentException(String.join("; ", result.errors()));
        }
        return miniMessage.deserialize(resolved);
    }

    private ValidationResult validateClickCommand(String input) {
        String value = input == null ? "" : input.toLowerCase();
        if (value.contains("<click:run_command>") || value.contains("<click:suggest_command>")) {
            return ValidationResult.invalid("Click command tags require a command value.");
        }
        return ValidationResult.ok();
    }
}
