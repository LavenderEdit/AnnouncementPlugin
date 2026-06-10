package dev.studio.announcer.common.avatar;

import dev.studio.announcer.api.avatar.AvatarResult;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AvatarCacheStore {

    CompletableFuture<Optional<AvatarResult>> find(UUID playerId);

    CompletableFuture<Void> save(AvatarResult result);
}
