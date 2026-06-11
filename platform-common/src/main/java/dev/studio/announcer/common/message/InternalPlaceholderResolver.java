package dev.studio.announcer.common.message;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.placeholder.PlaceholderResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public final class InternalPlaceholderResolver implements PlaceholderResolver {
    private final Clock clock;

    public InternalPlaceholderResolver() {
        this(Clock.systemDefaultZone());
    }

    public InternalPlaceholderResolver(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String resolve(String input, PlaceholderContext context) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        PlaceholderContext safeContext = context == null ? PlaceholderContext.empty() : context;
        String resolved = input.replace("%newline%", System.lineSeparator());
        resolved = resolved
                .replace("%time%", LocalTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_TIME))
                .replace("%date%", LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (safeContext.value("player_displayname").isEmpty()) {
            resolved = resolved.replace("%player_displayname%", safeContext.value("player_name").orElse(""));
        }
        for (Map.Entry<String, String> entry : safeContext.values().entrySet()) {
            resolved = resolved.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return resolved;
    }
}
