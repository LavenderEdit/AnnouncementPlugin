package dev.studio.announcer.common.message;

import java.util.Map;

public record PluginMessages(String language, Map<String, String> messages) {

    public PluginMessages {
        language = language == null || language.isBlank() ? "en" : language;
        messages = messages == null ? Map.of() : Map.copyOf(messages);
    }

    public String message(String key, String fallback) {
        return messages.getOrDefault(key, fallback);
    }
}
