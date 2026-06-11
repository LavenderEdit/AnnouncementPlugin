package dev.studio.announcer.common.config;

import java.nio.file.Path;
import java.util.Objects;

public record ConfigurationPaths(
        Path dataDirectory,
        Path configFile,
        Path avatarCacheFile,
        Path announcementsDirectory,
        Path menusDirectory,
        Path messagesDirectory,
        Path backupsDirectory) {

    public ConfigurationPaths {
        dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        configFile = Objects.requireNonNull(configFile, "configFile");
        avatarCacheFile = Objects.requireNonNull(avatarCacheFile, "avatarCacheFile");
        announcementsDirectory = Objects.requireNonNull(announcementsDirectory, "announcementsDirectory");
        menusDirectory = Objects.requireNonNull(menusDirectory, "menusDirectory");
        messagesDirectory = Objects.requireNonNull(messagesDirectory, "messagesDirectory");
        backupsDirectory = Objects.requireNonNull(backupsDirectory, "backupsDirectory");
    }

    public static ConfigurationPaths fromDataDirectory(Path dataDirectory) {
        Path root = Objects.requireNonNull(dataDirectory, "dataDirectory");
        return new ConfigurationPaths(
                root,
                root.resolve("config.yml"),
                root.resolve("avatar_cache.yml"),
                root.resolve("announcements"),
                root.resolve("menus"),
                root.resolve("messages"),
                root.resolve("backups"));
    }
}
