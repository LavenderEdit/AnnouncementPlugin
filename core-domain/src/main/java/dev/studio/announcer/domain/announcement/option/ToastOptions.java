package dev.studio.announcer.domain.announcement.option;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record ToastOptions(
        boolean enabled,
        String title,
        String description,
        String iconMaterial,
        ToastFrameType frameType,
        Duration duration,
        boolean dynamicPlayerHeadIcon,
        Integer customModelData,
        String permission) {

    public ToastOptions {
        title = title == null ? "" : title.trim();
        description = description == null ? "" : description.trim();
        iconMaterial = iconMaterial == null ? "" : iconMaterial.trim();
        frameType = Objects.requireNonNull(frameType, "frameType");
        if (enabled && title.isBlank()) {
            throw new IllegalArgumentException("Enabled Toast requires a title.");
        }
        if (enabled && iconMaterial.isBlank() && !dynamicPlayerHeadIcon) {
            throw new IllegalArgumentException("Enabled Toast requires an icon material or dynamic player head.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Toast duration must be positive.");
        }
        if (customModelData != null && customModelData < 0) {
            throw new IllegalArgumentException("Custom model data cannot be negative.");
        }
        permission = normalize(permission);
    }

    public Optional<String> permissionValue() {
        return Optional.ofNullable(permission);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
