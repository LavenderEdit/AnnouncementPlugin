package dev.studio.announcer.common.config;

public record AdvancedAnnouncerConfig(
        int configVersion,
        String language,
        boolean debug,
        String storageType,
        boolean schedulerEnabled,
        long defaultIntervalSeconds) {

    public static AdvancedAnnouncerConfig defaults() {
        return new AdvancedAnnouncerConfig(1, "en", false, "yaml", true, 300L);
    }
}
