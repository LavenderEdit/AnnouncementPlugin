package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.ConfigurationReloadService;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyConfigurationMigrationServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyConfigBacksUpAndReloads() throws Exception {
        ConfigurationPaths paths = ConfigurationPaths.fromDataDirectory(tempDir.resolve("plugin"));
        Files.createDirectories(paths.announcementsDirectory());
        Files.createDirectories(paths.backupsDirectory());
        Files.writeString(paths.announcementsDirectory().resolve("global.yml"), "announcements: {}\n");
        Path legacyConfig = tempDir.resolve("legacy-config.yml");
        Files.writeString(legacyConfig, """
                welcome:
                  enabled: true
                  first-join:
                    enabled: true
                    chat: "<green>Welcome %player%</green>"
                donations:
                  enabled: true
                  format: "<gold>%player% donated %item%</gold>"
                ranks:
                  enabled: false
                """);
        YamlAnnouncementRepository repository = new YamlAnnouncementRepository(paths.announcementsDirectory());
        RecordingReloadService reloadService = new RecordingReloadService();
        LegacyConfigurationMigrationService service = new LegacyConfigurationMigrationService(
                legacyConfig,
                paths,
                repository,
                new ConfigBackupService(paths.backupsDirectory()),
                reloadService,
                new LegacyWelcomeDonationsMigrator());

        ConfigurationReloadResult result = service.migrate();

        assertTrue(result.success());
        assertEquals(1, reloadService.calls);
        assertTrue(repository.findById(AnnouncementId.of("legacy_first_join")).isPresent());
        assertTrue(repository.findById(AnnouncementId.of("legacy_donation")).isPresent());
        try (var backups = Files.list(paths.backupsDirectory())) {
            assertTrue(backups.count() >= 2);
        }
    }

    private static final class RecordingReloadService implements ConfigurationReloadService {
        private int calls;

        @Override
        public ConfigurationReloadResult reload() {
            calls++;
            return ConfigurationReloadResult.success("Reloaded.");
        }
    }
}
