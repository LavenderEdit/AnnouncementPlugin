package dev.studio.announcer.api.audience;

import java.util.Objects;

public final class PlayerAudience implements Audience {
    private final String playerId;

    public PlayerAudience(String playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId cannot be blank");
        }
        this.playerId = playerId;
    }

    public String playerId() {
        return playerId;
    }
}
