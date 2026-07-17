package dev.studio.announcer.common.message;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.placeholder.PlaceholderResolver;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class MiniMessageComponentRenderer implements MessageRenderer<Component> {
    private static final int CHAT_WIDTH = 150;
    private static final Pattern CENTER_PATTERN = Pattern.compile(
            "<center>(.*?)</center>", Pattern.DOTALL);
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

    public PlaceholderResolver placeholderResolver() {
        return placeholderResolver;
    }

    @Override
    public ValidationResult validate(String input) {
        ValidationResult clickValidation = validateClickCommand(input);
        if (!clickValidation.valid()) {
            return clickValidation;
        }
        try {
            String preprocessed = preprocessCenter(input == null ? "" : input);
            miniMessage.deserialize(preprocessed);
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
        String preprocessed = preprocessCenter(resolved);
        return miniMessage.deserialize(preprocessed);
    }

    private String preprocessCenter(String input) {
        if (input == null || !input.contains("<center>")) {
            return input;
        }
        Matcher matcher = CENTER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String content = matcher.group(1);
            String plain = content.replaceAll("<[^>]+>", "");
            int contentLength = plain.length();
            int padding = Math.max(0, (CHAT_WIDTH - contentLength) / 2);
            String pad = " ".repeat(padding);
            String replacement = pad + content;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private ValidationResult validateClickCommand(String input) {
        if (input == null) {
            return ValidationResult.ok();
        }
        String lower = input.toLowerCase(Locale.ROOT);
        if ((lower.contains("<click:run_command>") && !lower.contains("<click:run_command:"))
                || (lower.contains("<click:suggest_command>") && !lower.contains("<click:suggest_command:"))) {
            return ValidationResult.invalid("Click command tags require a command value.");
        }
        return ValidationResult.ok();
    }
}
