package dev.studio.announcer.api.service;

public interface PlatformStatusService {

    String platformName();

    String platformVersion();

    default String pluginVersion() {
        return "unknown";
    }

    default boolean schedulerEnabled() {
        return true;
    }

    boolean foliaDetected();

    boolean placeholderApiAvailable();

    boolean redisEnabled();

    boolean discordEnabled();
}
