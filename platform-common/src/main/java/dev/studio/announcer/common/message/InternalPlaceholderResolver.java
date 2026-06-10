package dev.studio.announcer.common.message;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.placeholder.PlaceholderResolver;
import java.util.Map;

public final class InternalPlaceholderResolver implements PlaceholderResolver {

    @Override
    public String resolve(String input, PlaceholderContext context) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        PlaceholderContext safeContext = context == null ? PlaceholderContext.empty() : context;
        String resolved = input.replace("%newline%", System.lineSeparator());
        for (Map.Entry<String, String> entry : safeContext.values().entrySet()) {
            resolved = resolved.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return resolved;
    }
}
