package dev.studio.announcer.api.service;

import java.util.List;

public record ConfigurationReloadResult(boolean success, String message, List<String> errors) {

    public ConfigurationReloadResult {
        message = message == null || message.isBlank() ? (success ? "Reload completed." : "Reload failed.") : message;
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ConfigurationReloadResult success(String message) {
        return new ConfigurationReloadResult(true, message, List.of());
    }

    public static ConfigurationReloadResult failure(String message, List<String> errors) {
        return new ConfigurationReloadResult(false, message, errors);
    }
}
