package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlConfigurationReloadServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void reloadValidatesBeforeReplacingRuntimeRepository() throws Exception {
        ConfigurationPaths paths = ConfigurationPaths.fromDataDirectory(tempDir);
        new ConfigurationBootstrapper(paths).bootstrap();
        YamlAnnouncementRepository repository = new YamlAnnouncementRepository(paths.announcementsDirectory());
        repository.save(Announcement.builder(AnnouncementId.of("stable"), "Stable")
                .messages(List.of("<green>Stable</green>"))
                .build());

        Files.writeString(paths.announcementsDirectory().resolve("global.yml"), """
                announcements:
                  broken:
                    name: Broken
                    enabled: true
                    type: GLOBAL
                    channels:
                      - CHAT
                    messages:
                      - "<green>Broken"
                """);
        YamlConfigurationReloadService service = new YamlConfigurationReloadService(
                paths,
                repository,
                new ConfigurationValidationService(new MiniMessageComponentRenderer()));

        dev.studio.announcer.api.service.ConfigurationReloadResult result = service.reload();

        assertFalse(result.success());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("broken")));
        assertTrue(repository.findById(AnnouncementId.of("stable")).isPresent());
    }

    @Test
    void validReloadReplacesRepositoryState() throws Exception {
        ConfigurationPaths paths = ConfigurationPaths.fromDataDirectory(tempDir);
        new ConfigurationBootstrapper(paths).bootstrap();
        YamlAnnouncementRepository repository = new YamlAnnouncementRepository(paths.announcementsDirectory());
        Files.writeString(paths.announcementsDirectory().resolve("global.yml"), """
                announcements:
                  new_one:
                    name: New One
                    enabled: true
                    type: GLOBAL
                    channels:
                      - CHAT
                    messages:
                      - "<green>New</green>"
                """);
        YamlConfigurationReloadService service = new YamlConfigurationReloadService(
                paths,
                repository,
                new ConfigurationValidationService(new MiniMessageComponentRenderer()));

        dev.studio.announcer.api.service.ConfigurationReloadResult result = service.reload();

        assertTrue(result.success());
        assertEquals("New One", repository.findById(AnnouncementId.of("new_one")).orElseThrow().name());
    }
}
