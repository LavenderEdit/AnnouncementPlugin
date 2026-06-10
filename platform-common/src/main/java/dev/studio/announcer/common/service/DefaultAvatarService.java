package dev.studio.announcer.common.service;

import dev.studio.announcer.api.avatar.AvatarResult;
import dev.studio.announcer.api.service.AvatarService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultAvatarService implements AvatarService {

    @Override
    public CompletableFuture<AvatarResult> resolveAvatar(UUID playerId, String playerName) {
        return CompletableFuture.completedFuture(new AvatarResult(playerId, playerName, null, true));
    }
}
