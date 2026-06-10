package dev.studio.announcer.spigot.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.common.message.InternalPlaceholderResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpigotPlaceholderResolverTest {

    @Test
    void resolvesOnlyInternalPlaceholdersWhenPlaceholderApiIsAbsent() {
        SpigotPlaceholderResolver resolver = new SpigotPlaceholderResolver(new InternalPlaceholderResolver(), PlaceholderApiBridge.disabled());
        PlaceholderContext context = PlaceholderContext.of(Map.of("player_name", "Ari"));

        String resolved = resolver.resolve("Hello %player_name% %vault_rank%", context);

        assertEquals("Hello Ari %vault_rank%", resolved);
    }

    @Test
    void appliesPlaceholderApiAfterInternalPlaceholdersWhenAvailable() {
        PlaceholderApiBridge bridge = PlaceholderApiBridge.available((input, context) -> input.replace("%vault_rank%", "VIP"));
        SpigotPlaceholderResolver resolver = new SpigotPlaceholderResolver(new InternalPlaceholderResolver(), bridge);
        PlaceholderContext context = PlaceholderContext.of(Map.of("player_name", "Ari"));

        String resolved = resolver.resolve("Hello %player_name% %vault_rank%", context);

        assertEquals("Hello Ari VIP", resolved);
    }
}
