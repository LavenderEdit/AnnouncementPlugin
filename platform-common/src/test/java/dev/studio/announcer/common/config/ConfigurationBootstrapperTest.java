package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationBootstrapperTest {
    @TempDir
    Path tempDir;

    @Test
    void createsExpectedFilesWithoutOverwritingExistingConfig() throws Exception {
        ConfigurationPaths paths = ConfigurationPaths.fromDataDirectory(tempDir);
        Files.createDirectories(tempDir);
        Files.writeString(paths.configFile(), "language: es\n");

        new ConfigurationBootstrapper(paths).bootstrap();

        assertEquals("language: es\n", Files.readString(paths.configFile()));
        assertTrue(Files.exists(paths.announcementsDirectory().resolve("global.yml")));
        assertTrue(Files.exists(paths.announcementsDirectory().resolve("vip.yml")));
        assertTrue(Files.exists(paths.announcementsDirectory().resolve("events.yml")));
        assertTrue(Files.exists(paths.messagesDirectory().resolve("en.yml")));
        assertTrue(Files.exists(paths.messagesDirectory().resolve("es.yml")));
        assertTrue(Files.exists(paths.menusDirectory().resolve("main.yml")));
        assertTrue(Files.exists(paths.menusDirectory().resolve("editor.yml")));
        assertTrue(Files.exists(paths.avatarCacheFile()));
    }
}
