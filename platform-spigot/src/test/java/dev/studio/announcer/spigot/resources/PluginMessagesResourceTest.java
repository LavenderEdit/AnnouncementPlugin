package dev.studio.announcer.spigot.resources;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PluginMessagesResourceTest {
    private static final Set<String> REQUIRED_KEYS = Set.of(
            "reload-success",
            "reload-error",
            "no-permission",
            "migration-success",
            "migration-error",
            "editor-console-only",
            "editor-saved",
            "editor-cancelled",
            "redis-disabled",
            "redis-connected",
            "discord-disabled",
            "discord-test-sent",
            "command-version",
            "command-debug",
            "announcement-created",
            "announcement-deleted",
            "announcement-toggled",
            "announcement-sent",
            "announcement-previewed",
            "announcement-list-empty",
            "announcement-not-found");

    @Test
    void bundledEnglishAndSpanishMessagesExposeRequiredKeys() {
        assertContainsRequiredKeys("messages/en.yml");
        assertContainsRequiredKeys("messages/es.yml");
    }

    private static void assertContainsRequiredKeys(String resource) {
        String content = readResource(resource);
        for (String key : REQUIRED_KEYS) {
            assertTrue(content.lines().anyMatch(line -> line.startsWith(key + ":")),
                    () -> resource + " is missing key " + key);
        }
    }

    private static String readResource(String resource) {
        try (var input = PluginMessagesResourceTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
