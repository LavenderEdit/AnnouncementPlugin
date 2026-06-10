package dev.studio.announcer.spigot.service;

import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AdventureActionBarSender implements ActionBarSender {
    private final BukkitAudiences audiences;

    public AdventureActionBarSender(BukkitAudiences audiences) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
    }

    @Override
    public void show(String audienceId, Component message) {
        findPlayer(audienceId).ifPresent(player -> audiences.player(player).sendActionBar(message));
    }

    @Override
    public void clear(String audienceId) {
        findPlayer(audienceId).ifPresent(player -> audiences.player(player).sendActionBar(Component.empty()));
    }

    private java.util.Optional<Player> findPlayer(String audienceId) {
        try {
            return java.util.Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(audienceId)));
        } catch (IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
    }
}
