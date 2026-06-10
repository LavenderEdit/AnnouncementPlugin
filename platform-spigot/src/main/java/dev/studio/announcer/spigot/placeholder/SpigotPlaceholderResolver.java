package dev.studio.announcer.spigot.placeholder;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.placeholder.PlaceholderResolver;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SpigotPlaceholderResolver implements PlaceholderResolver {
    private final PlaceholderResolver internalResolver;
    private final PlaceholderApiBridge placeholderApiBridge;

    public SpigotPlaceholderResolver(PlaceholderResolver internalResolver, PlaceholderApiBridge placeholderApiBridge) {
        this.internalResolver = internalResolver;
        this.placeholderApiBridge = placeholderApiBridge == null ? PlaceholderApiBridge.disabled() : placeholderApiBridge;
    }

    @Override
    public String resolve(String input, PlaceholderContext context) {
        PlaceholderContext safeContext = context == null ? PlaceholderContext.empty() : context;
        String resolved = internalResolver.resolve(input, safeContext);
        if (!placeholderApiBridge.available()) {
            return resolved;
        }
        return placeholderApiBridge.apply(resolved, safeContext);
    }

    static Optional<Player> playerByUuid(String value) {
        try {
            return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(value)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
