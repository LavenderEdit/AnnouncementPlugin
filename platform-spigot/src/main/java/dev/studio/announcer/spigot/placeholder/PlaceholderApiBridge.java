package dev.studio.announcer.spigot.placeholder;

import dev.studio.announcer.api.placeholder.PlaceholderContext;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderApiBridge {
    private final boolean available;
    private final PlaceholderTransformer transformer;

    private PlaceholderApiBridge(boolean available, PlaceholderTransformer transformer) {
        this.available = available;
        this.transformer = transformer;
    }

    public static PlaceholderApiBridge disabled() {
        return new PlaceholderApiBridge(false, (input, context) -> input);
    }

    public static PlaceholderApiBridge available(PlaceholderTransformer transformer) {
        return new PlaceholderApiBridge(true, transformer);
    }

    public static PlaceholderApiBridge detect() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return disabled();
        }
        try {
            Class<?> apiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method setPlaceholders = apiClass.getMethod("setPlaceholders", Player.class, String.class);
            return available((input, context) -> {
                Player player = context.value("player_uuid")
                        .flatMap(SpigotPlaceholderResolver::playerByUuid)
                        .or(() -> context.value("player_name").map(Bukkit::getPlayerExact))
                        .orElse(null);
                if (player == null) {
                    return input;
                }
                try {
                    Object resolved = setPlaceholders.invoke(null, player, input);
                    return resolved == null ? input : resolved.toString();
                } catch (ReflectiveOperationException ex) {
                    return input;
                }
            });
        } catch (ReflectiveOperationException ex) {
            return disabled();
        }
    }

    public boolean available() {
        return available;
    }

    public String apply(String input, PlaceholderContext context) {
        return transformer.apply(input, context);
    }

    @FunctionalInterface
    public interface PlaceholderTransformer {
        String apply(String input, PlaceholderContext context);
    }
}
