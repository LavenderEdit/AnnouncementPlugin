package dev.studio.announcer.spigot.service;

import dev.studio.announcer.api.service.SoundService;
import dev.studio.announcer.domain.announcement.option.SoundOptions;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SpigotSoundService implements SoundService {

    @Override
    public boolean validSound(String key) {
        return resolveSound(key) != null;
    }

    @Override
    public boolean play(String audienceId, SoundOptions options) {
        if (options == null) {
            return false;
        }
        Player player = Bukkit.getPlayerExact(audienceId);
        if (player == null) {
            try {
                player = Bukkit.getPlayer(java.util.UUID.fromString(audienceId));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        Sound sound = resolveSound(options.key());
        if (player == null || sound == null) {
            return false;
        }
        player.playSound(player.getLocation(), sound, options.volume(), options.pitch());
        return true;
    }

    private Sound resolveSound(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        NamespacedKey namespacedKey = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        if (namespacedKey != null) {
            Sound sound = Registry.SOUNDS.get(namespacedKey);
            if (sound != null) {
                return sound;
            }
        }
        try {
            return Sound.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
