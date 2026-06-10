package dev.studio.announcer.spigot.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitToastSender implements ToastSender {
    private final JavaPlugin plugin;

    public BukkitToastSender(JavaPlugin plugin) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void addToast(String audienceId, VirtualToast toast) {
        NamespacedKey key = key(toast.key());
        try {
            Bukkit.getUnsafe().removeAdvancement(key);
        } catch (IllegalArgumentException ignored) {
            // Advancement was not registered.
        }
        Bukkit.getUnsafe().loadAdvancement(key, AdvancementToastJson.build(toast));
    }

    @Override
    public void grantToast(String audienceId, String toastKey) {
        Optional<Player> player = findPlayer(audienceId);
        if (player.isEmpty()) {
            return;
        }
        Advancement advancement = Bukkit.getAdvancement(key(toastKey));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.orElseThrow().getAdvancementProgress(advancement);
        for (String criteria : progress.getRemainingCriteria()) {
            progress.awardCriteria(criteria);
        }
    }

    @Override
    public void removeToast(String audienceId, String toastKey) {
        try {
            Bukkit.getUnsafe().removeAdvancement(key(toastKey));
        } catch (IllegalArgumentException ignored) {
            // Advancement was already removed.
        }
    }

    private Optional<Player> findPlayer(String audienceId) {
        try {
            return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(audienceId)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private NamespacedKey key(String rawKey) {
        String normalized = rawKey == null ? "" : rawKey.toLowerCase(Locale.ROOT);
        NamespacedKey parsed = NamespacedKey.fromString(normalized, plugin);
        if (parsed != null) {
            return parsed;
        }
        return new NamespacedKey(plugin, normalized.replace(':', '/'));
    }
}
