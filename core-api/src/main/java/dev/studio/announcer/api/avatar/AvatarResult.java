package dev.studio.announcer.api.avatar;

import java.util.Optional;
import java.util.UUID;

public record AvatarResult(UUID playerId, String playerName, String textureBase64, boolean fallback) {

    public Optional<String> texture() {
        if (textureBase64 == null || textureBase64.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(textureBase64);
    }
}
