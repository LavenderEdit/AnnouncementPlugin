package dev.studio.announcer.spigot.service;

import dev.studio.announcer.api.service.PlatformStatusService;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;

public final class SpigotPlatformStatusService implements PlatformStatusService {
    private final String pluginVersion;
    private final BooleanSupplier schedulerEnabled;
    private final BooleanSupplier redisEnabled;
    private final BooleanSupplier discordEnabled;

    public SpigotPlatformStatusService() {
        this("unknown", () -> true, () -> false, () -> false);
    }

    public SpigotPlatformStatusService(BooleanSupplier redisEnabled, BooleanSupplier discordEnabled) {
        this("unknown", () -> true, redisEnabled, discordEnabled);
    }

    public SpigotPlatformStatusService(
            String pluginVersion,
            BooleanSupplier schedulerEnabled,
            BooleanSupplier redisEnabled,
            BooleanSupplier discordEnabled) {
        this.pluginVersion = pluginVersion == null || pluginVersion.isBlank() ? "unknown" : pluginVersion.trim();
        this.schedulerEnabled = schedulerEnabled == null ? () -> true : schedulerEnabled;
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
    public String pluginVersion() {
        return pluginVersion;
    }

    @Override
    public boolean schedulerEnabled() {
        return schedulerEnabled.getAsBoolean();
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
