package dev.studio.announcer.spigot.service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AdventureBossBarSender implements BossBarSender {
    private final BukkitAudiences audiences;

    public AdventureBossBarSender(BukkitAudiences audiences) {
        this.audiences = Objects.requireNonNull(audiences, "audiences");
    }

    @Override
    public void show(String audienceId, BossBar bossBar) {
        findPlayer(audienceId).ifPresent(player -> audiences.player(player).showBossBar(bossBar));
    }

    @Override
    public void hide(String audienceId, BossBar bossBar) {
        findPlayer(audienceId).ifPresent(player -> audiences.player(player).hideBossBar(bossBar));
    }

    private Optional<Player> findPlayer(String audienceId) {
        try {
            return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(audienceId)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
