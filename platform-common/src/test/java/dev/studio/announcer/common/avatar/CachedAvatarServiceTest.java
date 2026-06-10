package dev.studio.announcer.common.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.avatar.AvatarResult;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CachedAvatarServiceTest {
    private static final UUID PLAYER_ID = UUID.fromString("c456fb2f-6b64-4f61-bf4b-9ac113d3e2c1");

    @Test
    void l1CacheAvoidsRepeatedSourceLookups() {
        InMemoryAvatarCacheStore store = new InMemoryAvatarCacheStore();
        CountingAvatarSource source = new CountingAvatarSource(Optional.of(result("texture")));
        CachedAvatarService service = new CachedAvatarService(store, source);

        AvatarResult first = service.resolveAvatar(PLAYER_ID, "Ada").join();
        AvatarResult second = service.resolveAvatar(PLAYER_ID, "Ada").join();

        assertEquals("texture", first.texture().orElseThrow());
        assertEquals(first, second);
        assertEquals(1, source.calls());
    }

    @Test
    void l2StoreHitAvoidsSourceLookup() {
        InMemoryAvatarCacheStore store = new InMemoryAvatarCacheStore();
        store.save(result("stored")).join();
        CountingAvatarSource source = new CountingAvatarSource(Optional.of(result("source")));
        CachedAvatarService service = new CachedAvatarService(store, source);

        AvatarResult resolved = service.resolveAvatar(PLAYER_ID, "Ada").join();

        assertEquals("stored", resolved.texture().orElseThrow());
        assertEquals(0, source.calls());
    }

    @Test
    void sourceHitIsSavedToStore() {
        InMemoryAvatarCacheStore store = new InMemoryAvatarCacheStore();
        CountingAvatarSource source = new CountingAvatarSource(Optional.of(result("source")));
        CachedAvatarService service = new CachedAvatarService(store, source);

        AvatarResult resolved = service.resolveAvatar(PLAYER_ID, "Ada").join();

        assertEquals("source", resolved.texture().orElseThrow());
        assertEquals("source", store.find(PLAYER_ID).join().orElseThrow().texture().orElseThrow());
    }

    @Test
    void missingTextureReturnsFallbackAvatar() {
        CachedAvatarService service = new CachedAvatarService(
                new InMemoryAvatarCacheStore(),
                new CountingAvatarSource(Optional.empty()));

        AvatarResult resolved = service.resolveAvatar(PLAYER_ID, "Ada").join();

        assertEquals(PLAYER_ID, resolved.playerId());
        assertEquals("Ada", resolved.playerName());
        assertTrue(resolved.fallback());
        assertFalse(resolved.texture().isPresent());
    }

    private static AvatarResult result(String texture) {
        return new AvatarResult(PLAYER_ID, "Ada", texture, false);
    }

    private static final class CountingAvatarSource implements AvatarTextureSource {
        private final Optional<AvatarResult> result;
        private int calls;

        private CountingAvatarSource(Optional<AvatarResult> result) {
            this.result = result;
        }

        @Override
        public CompletableFuture<Optional<AvatarResult>> resolve(UUID playerId, String playerName) {
            calls++;
            return CompletableFuture.completedFuture(result);
        }

        private int calls() {
            return calls;
        }
    }
}
