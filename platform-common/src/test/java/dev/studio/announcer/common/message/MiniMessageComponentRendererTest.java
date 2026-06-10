package dev.studio.announcer.common.message;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class MiniMessageComponentRendererTest {

    @Test
    void validatesMiniMessageInput() {
        MiniMessageComponentRenderer renderer = new MiniMessageComponentRenderer();

        ValidationResult result = renderer.validate("<gradient:#ff0000:#0000ff>Hello</gradient>");

        assertTrue(result.valid(), () -> String.join(", ", result.errors()));
    }

    @Test
    void reportsInvalidMiniMessageInput() {
        MiniMessageComponentRenderer renderer = new MiniMessageComponentRenderer();

        ValidationResult result = renderer.validate("<click:run_command>Broken</click>");

        assertFalse(result.valid());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void rendersComponentAfterResolvingPlaceholders() {
        MiniMessageComponentRenderer renderer = new MiniMessageComponentRenderer(new InternalPlaceholderResolver());
        PlaceholderContext context = PlaceholderContext.of(Map.of("player_name", "Ari"));

        Component component = renderer.render("<green>Hello %player_name%</green>", context);

        assertNotNull(component);
    }
}
