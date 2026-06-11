package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.validation.ValidationResult;
import net.kyori.adventure.text.Component;

public final class AnvilTextInputService {
    private final MessageRenderer<Component> renderer;

    public AnvilTextInputService(MessageRenderer<Component> renderer) {
        this.renderer = renderer;
    }

    public ValidationResult validateMiniMessage(String input) {
        return renderer.validate(input);
    }

    public AnnouncementId sanitizeAnnouncementId(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(java.util.Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9_-]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "announcement";
        }
        return AnnouncementId.of(normalized);
    }
}
