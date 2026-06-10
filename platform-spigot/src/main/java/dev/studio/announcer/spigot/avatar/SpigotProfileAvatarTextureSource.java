package dev.studio.announcer.spigot.avatar;

import dev.studio.announcer.api.avatar.AvatarResult;
import dev.studio.announcer.common.avatar.AvatarTextureSource;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SpigotProfileAvatarTextureSource implements AvatarTextureSource {

    @Override
    public CompletableFuture<Optional<AvatarResult>> resolve(UUID playerId, String playerName) {
        return CompletableFuture.completedFuture(resolveOnlineProfile(playerId, playerName));
    }

    private Optional<AvatarResult> resolveOnlineProfile(UUID playerId, String playerName) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return Optional.empty();
        }
        return textureFromProfile(player)
                .map(texture -> new AvatarResult(playerId, playerName, texture, false));
    }

    private Optional<String> textureFromProfile(Player player) {
        try {
            Object profile = player.getClass().getMethod("getProfile").invoke(player);
            Method getProperties = profile.getClass().getMethod("getProperties");
            Object properties = getProperties.invoke(profile);
            Object textureProperties = properties.getClass().getMethod("get", Object.class).invoke(properties, "textures");
            if (!(textureProperties instanceof Collection<?> collection) || collection.isEmpty()) {
                return Optional.empty();
            }
            Iterator<?> iterator = collection.iterator();
            Object textureProperty = iterator.next();
            Object value = textureProperty.getClass().getMethod("getValue").invoke(textureProperty);
            if (value instanceof String texture && !texture.isBlank()) {
                return Optional.of(texture);
            }
            return Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return Optional.empty();
        }
    }
}
