package dev.studio.announcer.spigot.service;

import java.util.Locale;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

final class AdvancementToastJson {
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private AdvancementToastJson() {
    }

    static String build(VirtualToast toast) {
        String icon = normalizeIcon(toast);
        String customModelData = toast.customModelData() == null
                ? ""
                : ",\"components\":{\"minecraft:custom_model_data\":" + toast.customModelData() + "}";
        return "{"
                + "\"criteria\":{\"trigger\":{\"trigger\":\"minecraft:impossible\"}},"
                + "\"display\":{"
                + "\"icon\":{\"id\":\"" + escape(icon) + "\"" + customModelData + "},"
                + "\"title\":" + GSON.serialize(toast.title()) + ","
                + "\"description\":" + GSON.serialize(toast.description()) + ","
                + "\"frame\":\"" + toast.frameType().name().toLowerCase(Locale.ROOT) + "\","
                + "\"show_toast\":true,"
                + "\"announce_to_chat\":false,"
                + "\"hidden\":true"
                + "}"
                + "}";
    }

    private static String normalizeIcon(VirtualToast toast) {
        if (toast.dynamicPlayerHeadIcon()) {
            return "minecraft:player_head";
        }
        String icon = toast.iconMaterial();
        if (icon == null || icon.isBlank()) {
            return "minecraft:paper";
        }
        String normalized = icon.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (normalized.indexOf(':') < 0) {
            return "minecraft:" + normalized;
        }
        return normalized;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
