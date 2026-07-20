package dev.studio.announcer.common.avatar;

import dev.studio.announcer.api.avatar.AvatarResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAvatarCacheStore implements AvatarCacheStore {
    private static final int MAX_ENTRIES = 5000;
    private final Map<UUID, AvatarResult> results = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Optional<AvatarResult>> find(UUID playerId) {
        return CompletableFuture.completedFuture(Optional.ofNullable(results.get(playerId)));
    }

    @Override
    public CompletableFuture<Void> save(AvatarResult result) {
        if (result != null && result.playerId() != null && result.texture().isPresent()) {
            if (results.size() >= MAX_ENTRIES) {
                results.clear();
            }
            results.put(result.playerId(), result);
        }
        return CompletableFuture.completedFuture(null);
    }
}
