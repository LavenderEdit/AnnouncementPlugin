package dev.studio.announcer.spigot.service;

import dev.studio.announcer.domain.announcement.option.ToastFrameType;
import java.util.Objects;
import net.kyori.adventure.text.Component;

public record VirtualToast(
        String key,
        Component title,
        Component description,
        String iconMaterial,
        ToastFrameType frameType,
        boolean dynamicPlayerHeadIcon,
        Integer customModelData) {

    public VirtualToast {
        key = requireNonBlank(key, "Toast key");
        title = Objects.requireNonNull(title, "title");
        description = description == null ? Component.empty() : description;
        iconMaterial = iconMaterial == null ? "" : iconMaterial.trim();
        frameType = Objects.requireNonNull(frameType, "frameType");
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank.");
        }
        return value.trim();
    }
}
