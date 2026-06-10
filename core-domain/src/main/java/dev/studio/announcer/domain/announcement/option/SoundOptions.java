package dev.studio.announcer.domain.announcement.option;

public record SoundOptions(String key, float volume, float pitch) {

    public SoundOptions {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Sound key cannot be blank.");
        }
        key = key.trim();
        if (volume < 0.0f) {
            throw new IllegalArgumentException("Sound volume cannot be negative.");
        }
        if (pitch <= 0.0f) {
            throw new IllegalArgumentException("Sound pitch must be positive.");
        }
    }
}
