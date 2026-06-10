package dev.studio.announcer.spigot.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.domain.announcement.option.ToastFrameType;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class AdvancementToastJsonTest {

    @Test
    void buildsVirtualAdvancementJsonForToast() {
        VirtualToast toast = new VirtualToast(
                "advancedannouncer:toast/player/1",
                Component.text("Donation"),
                Component.text("Thanks"),
                "DIAMOND",
                ToastFrameType.CHALLENGE,
                false,
                null);

        String json = AdvancementToastJson.build(toast);

        assertTrue(json.contains("\"show_toast\":true"));
        assertTrue(json.contains("\"announce_to_chat\":false"));
        assertTrue(json.contains("\"frame\":\"challenge\""));
        assertTrue(json.contains("\"minecraft:diamond\""));
        assertTrue(json.contains("\"Donation\""));
        assertTrue(json.contains("\"Thanks\""));
    }
}
