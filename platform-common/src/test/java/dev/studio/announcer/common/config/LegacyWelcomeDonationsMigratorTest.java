package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyWelcomeDonationsMigratorTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsWelcomeDonationsAndRanksIntoAnnouncements() throws Exception {
        Path legacy = tempDir.resolve("legacy.yml");
        Files.writeString(legacy, """
                welcome:
                  enabled: true
                  first-join:
                    enabled: true
                    chat: "<green>Welcome %player%</green>"
                  rejoin:
                    chat: "<yellow>Back %player%</yellow>"
                donations:
                  enabled: true
                  format: "<gold>%player% donated %item%</gold>"
                  sound: "ENTITY_VILLAGER_CELEBRATE"
                ranks:
                  enabled: true
                  format: "&6%player% bought %rank%"
                  sound: "ENTITY_PLAYER_LEVELUP"
                """);

        LegacyWelcomeDonationsMigrator migrator = new LegacyWelcomeDonationsMigrator();
        java.util.List<dev.studio.announcer.domain.announcement.Announcement> migrated = migrator.migrate(legacy);

        assertEquals(4, migrated.size());
        assertTrue(migrated.stream().anyMatch(item -> item.id().value().equals("legacy_first_join")
                && item.type() == AnnouncementType.EVENT_JOIN));
        assertTrue(migrated.stream().anyMatch(item -> item.id().value().equals("legacy_donation")
                && item.soundOptions().isPresent()));
        assertTrue(migrated.stream().anyMatch(item -> item.messages().getFirst().contains("%rank%")));
    }
}
