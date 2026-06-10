package dev.studio.announcer.api.service;

import dev.studio.announcer.api.avatar.AvatarResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AvatarService {

    CompletableFuture<AvatarResult> resolveAvatar(UUID playerId, String playerName);
}
