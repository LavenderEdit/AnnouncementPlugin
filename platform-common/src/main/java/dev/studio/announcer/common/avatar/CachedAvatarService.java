package dev.studio.announcer.common.avatar;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.studio.announcer.api.avatar.AvatarResult;
import dev.studio.announcer.api.service.AvatarService;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CachedAvatarService implements AvatarService {
    private final AvatarCacheStore store;
    private final AvatarTextureSource source;
    private final Cache<UUID, CompletableFuture<AvatarResult>> l1Cache;

    public CachedAvatarService(AvatarCacheStore store, AvatarTextureSource source) {
        this(store, source, Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(Duration.ofHours(6))
                .build());
    }

    public CachedAvatarService(
            AvatarCacheStore store,
            AvatarTextureSource source,
            Cache<UUID, CompletableFuture<AvatarResult>> l1Cache) {
        this.store = Objects.requireNonNull(store, "store");
        this.source = Objects.requireNonNull(source, "source");
        this.l1Cache = Objects.requireNonNull(l1Cache, "l1Cache");
    }

    @Override
    public CompletableFuture<AvatarResult> resolveAvatar(UUID playerId, String playerName) {
        UUID safePlayerId = Objects.requireNonNull(playerId, "playerId");
        String safePlayerName = playerName == null || playerName.isBlank() ? "unknown" : playerName.trim();
        CompletableFuture<AvatarResult> result = l1Cache.get(
                safePlayerId,
                ignored -> resolveFromLayers(safePlayerId, safePlayerName));
        result.whenComplete((ignored, error) -> {
            if (error != null) {
                l1Cache.invalidate(safePlayerId);
            }
        });
        return result;
    }

    private CompletableFuture<AvatarResult> resolveFromLayers(UUID playerId, String playerName) {
        return store.find(playerId)
                .thenCompose(stored -> stored
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> resolveFromSource(playerId, playerName)))
                .exceptionally(error -> fallback(playerId, playerName));
    }

    private CompletableFuture<AvatarResult> resolveFromSource(UUID playerId, String playerName) {
        return source.resolve(playerId, playerName)
                .thenCompose(result -> result
                        .filter(value -> value.texture().isPresent())
                        .map(this::saveAndReturn)
                        .orElseGet(() -> CompletableFuture.completedFuture(fallback(playerId, playerName))));
    }

    private CompletableFuture<AvatarResult> saveAndReturn(AvatarResult result) {
        return store.save(result).handle((ignored, error) -> result);
    }

    private AvatarResult fallback(UUID playerId, String playerName) {
        return new AvatarResult(playerId, playerName, null, true);
    }
}
