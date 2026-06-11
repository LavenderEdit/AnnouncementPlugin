package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigBackupServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsTimestampedBackupBeforeOverwrite() throws Exception {
        Path source = tempDir.resolve("announcements").resolve("global.yml");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "announcements: {}\n");

        Path backup = new ConfigBackupService(tempDir.resolve("backups")).backup(source);

        assertTrue(Files.exists(backup));
        assertTrue(backup.getFileName().toString().startsWith("global-"));
        assertEquals("announcements: {}\n", Files.readString(backup));
    }
}
