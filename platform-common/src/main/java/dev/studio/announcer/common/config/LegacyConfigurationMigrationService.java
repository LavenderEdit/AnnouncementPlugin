package dev.studio.announcer.common.config;

import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.ConfigurationReloadService;
import dev.studio.announcer.api.service.LegacyMigrationService;
import dev.studio.announcer.domain.announcement.Announcement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class LegacyConfigurationMigrationService implements LegacyMigrationService {
    private final Path legacyConfig;
    private final Path announcementsDirectory;
    private final AnnouncementRepository repository;
    private final ConfigBackupService backupService;
    private final ConfigurationReloadService reloadService;
    private final LegacyWelcomeDonationsMigrator migrator;

    public LegacyConfigurationMigrationService(
            Path legacyConfig,
            ConfigurationPaths paths,
            AnnouncementRepository repository,
            ConfigBackupService backupService,
            ConfigurationReloadService reloadService,
            LegacyWelcomeDonationsMigrator migrator) {
        this.legacyConfig = Objects.requireNonNull(legacyConfig, "legacyConfig");
        this.announcementsDirectory = Objects.requireNonNull(paths, "paths").announcementsDirectory();
        this.repository = Objects.requireNonNull(repository, "repository");
        this.backupService = Objects.requireNonNull(backupService, "backupService");
        this.reloadService = Objects.requireNonNull(reloadService, "reloadService");
        this.migrator = Objects.requireNonNull(migrator, "migrator");
    }

    @Override
    public ConfigurationReloadResult migrate() {
        if (Files.notExists(legacyConfig)) {
            return ConfigurationReloadResult.failure(
                    "Legacy migration failed.",
                    List.of("Legacy config not found: " + legacyConfig));
        }
        try {
            backupService.backup(legacyConfig);
            Path globalAnnouncements = announcementsDirectory.resolve("global.yml");
            if (Files.exists(globalAnnouncements)) {
                backupService.backup(globalAnnouncements);
            }

            List<Announcement> migrated = migrator.migrate(legacyConfig);
            migrated.forEach(repository::save);
            if (migrated.isEmpty()) {
                return ConfigurationReloadResult.success("No legacy announcements were found to migrate.");
            }

            ConfigurationReloadResult reload = reloadService.reload();
            if (!reload.success()) {
                return ConfigurationReloadResult.failure(
                        "Legacy announcements were written, but reload failed.",
                        reload.errors());
            }
            return ConfigurationReloadResult.success("Migrated " + migrated.size() + " legacy announcement(s).");
        } catch (RuntimeException ex) {
            return ConfigurationReloadResult.failure(
                    "Legacy migration failed.",
                    List.of(ex.getMessage()));
        }
    }
}
