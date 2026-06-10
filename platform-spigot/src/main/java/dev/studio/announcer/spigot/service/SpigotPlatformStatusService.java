package dev.studio.announcer.spigot.service;

import dev.studio.announcer.api.service.PlatformStatusService;
import org.bukkit.Bukkit;

public final class SpigotPlatformStatusService implements PlatformStatusService {

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
        return false;
    }

    @Override
    public boolean discordEnabled() {
        return false;
    }
}
