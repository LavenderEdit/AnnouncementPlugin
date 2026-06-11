package dev.studio.announcer.spigot.service;

import dev.studio.announcer.api.service.PlatformStatusService;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;

public final class SpigotPlatformStatusService implements PlatformStatusService {
    private final BooleanSupplier redisEnabled;
    private final BooleanSupplier discordEnabled;

    public SpigotPlatformStatusService() {
        this(() -> false, () -> false);
    }

    public SpigotPlatformStatusService(BooleanSupplier redisEnabled, BooleanSupplier discordEnabled) {
        this.redisEnabled = redisEnabled == null ? () -> false : redisEnabled;
        this.discordEnabled = discordEnabled == null ? () -> false : discordEnabled;
    }

    @Override
    public String platformName() {
        return Bukkit.getName();
    }

    @Override
    public String platformVersion() {
        return Bukkit.getVersion();
    }

    @Override
    public boolean foliaDetected() {
        try {
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    @Override
    public boolean placeholderApiAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @Override
    public boolean redisEnabled() {
        return redisEnabled.getAsBoolean();
    }

    @Override
    public boolean discordEnabled() {
        return discordEnabled.getAsBoolean();
    }
}
