package dev.studio.announcer.spigot.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import org.junit.jupiter.api.Test;

class AnvilTextInputServiceTest {

    @Test
    void validMiniMessageInputPasses() {
        AnvilTextInputService inputService = new AnvilTextInputService(new MiniMessageComponentRenderer());

        assertTrue(inputService.validateMiniMessage("<green>Hello</green>").valid());
    }

    @Test
    void malformedMiniMessageInputFails() {
        AnvilTextInputService inputService = new AnvilTextInputService(new MiniMessageComponentRenderer());

        assertFalse(inputService.validateMiniMessage("<green>Hello").valid());
    }

    @Test
    void sanitizesAnnouncementIdsFromTypedInput() {
        AnvilTextInputService inputService = new AnvilTextInputService(new MiniMessageComponentRenderer());

        assertTrue(inputService.sanitizeAnnouncementId(" Summer Sale! ").value().equals("summer_sale"));
    }
}
