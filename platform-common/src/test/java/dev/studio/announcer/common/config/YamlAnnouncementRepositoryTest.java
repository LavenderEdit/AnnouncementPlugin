package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlAnnouncementRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesFindsListsDeletesAndReloadsAnnouncements() {
        YamlAnnouncementRepository repository = new YamlAnnouncementRepository(tempDir);
        Announcement announcement = Announcement.builder(AnnouncementId.of("sale"), "Sale")
                .messages(List.of("<green>Sale live</green>"))
                .build();

        repository.save(announcement);

        assertTrue(Files.exists(tempDir.resolve("global.yml")));
        assertEquals("sale", repository.findById(AnnouncementId.of("sale")).orElseThrow().id().value());
        assertEquals(List.of("sale"), repository.findAll().stream().map(item -> item.id().value()).toList());

        YamlAnnouncementRepository reloaded = new YamlAnnouncementRepository(tempDir);
        assertEquals("Sale", reloaded.findById(AnnouncementId.of("sale")).orElseThrow().name());

        assertTrue(reloaded.deleteById(AnnouncementId.of("sale")));
        assertFalse(reloaded.findById(AnnouncementId.of("sale")).isPresent());
    }
}
