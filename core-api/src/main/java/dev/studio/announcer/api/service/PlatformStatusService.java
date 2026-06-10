package dev.studio.announcer.api.service;

public interface PlatformStatusService {

    String platformName();

    String platformVersion();

    boolean foliaDetected();

    boolean placeholderApiAvailable();

    boolean redisEnabled();

    boolean discordEnabled();
}
