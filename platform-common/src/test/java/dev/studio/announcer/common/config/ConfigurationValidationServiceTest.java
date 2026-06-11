package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.common.message.PluginMessages;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.option.SoundOptions;
import dev.studio.announcer.domain.announcement.option.ToastFrameType;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigurationValidationServiceTest {

    @Test
    void acceptsValidAnnouncementMessages() {
        ConfigurationValidationService service = new ConfigurationValidationService(new MiniMessageComponentRenderer());
        Announcement announcement = Announcement.builder(AnnouncementId.of("ok"), "Ok")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Hello</green>"))
                .build();

        assertTrue(service.validateAnnouncements(List.of(announcement)).valid());
    }

    @Test
    void rejectsMalformedMiniMessageWithAnnouncementId() {
        ConfigurationValidationService service = new ConfigurationValidationService(new MiniMessageComponentRenderer());
        Announcement announcement = Announcement.builder(AnnouncementId.of("bad"), "Bad")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Hello"))
                .build();

        assertFalse(service.validateAnnouncements(List.of(announcement)).valid());
        assertTrue(service.validateAnnouncements(List.of(announcement)).errors().getFirst().contains("bad"));
    }

    @Test
    void rejectsMalformedCronExpression() {
        ConfigurationValidationService service = new ConfigurationValidationService(new MiniMessageComponentRenderer());
        Announcement announcement = Announcement.builder(AnnouncementId.of("cron_bad"), "Cron Bad")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Hello</green>"))
                .cronExpression("not enough")
                .build();

        assertFalse(service.validateAnnouncements(List.of(announcement)).valid());
        assertTrue(service.validateAnnouncements(List.of(announcement)).errors().stream()
                .anyMatch(error -> error.contains("cron_bad") && error.contains("cron")));
    }

    @Test
    void rejectsCronExpressionWithUnsafeCharacters() {
        ConfigurationValidationService service = new ConfigurationValidationService(new MiniMessageComponentRenderer());
        Announcement announcement = Announcement.builder(AnnouncementId.of("cron_chars"), "Cron Chars")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Hello</green>"))
                .cronExpression("* * * * @@@")
                .build();

        ValidationAssert.assertContains(
                service.validateAnnouncements(List.of(announcement)),
                "cron_chars",
                "cron");
    }

    @Test
    void rejectsUnsafeAnnouncementMetadataValues() {
        ConfigurationValidationService service = new ConfigurationValidationService(new MiniMessageComponentRenderer());
        Announcement announcement = Announcement.builder(AnnouncementId.of("unsafe"), "Unsafe")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Hello</green>"))
                .permission("announcer bad")
                .targetServers(Set.of("lobby 01"))
                .targetGroups(Set.of("survival/one"))
                .soundOptions(new SoundOptions("bad sound", 1.0f, 1.0f))
                .toastOptions(new ToastOptions(
                        true,
                        "Toast",
                        "Description",
                        "bad material!",
                        ToastFrameType.TASK,
                        Duration.ofSeconds(3),
                        false,
                        null,
                        "toast bad"))
                .build();

        var result = service.validateAnnouncements(List.of(announcement));

        ValidationAssert.assertContains(result, "permission");
        ValidationAssert.assertContains(result, "target server");
        ValidationAssert.assertContains(result, "target group");
        ValidationAssert.assertContains(result, "sound");
        ValidationAssert.assertContains(result, "material");
    }

    @Test
    void validatesRedisAndDiscordIntegrationSettings() {
        ConfigurationValidationService service = new ConfigurationValidationService(new MiniMessageComponentRenderer());

        ValidationAssert.assertContains(
                service.validateRedisSettings(true, "not a uri", "", 0),
                "Redis URI");
        ValidationAssert.assertContains(
                service.validateDiscordSettings(true, true, "ftp://discord.test/webhook", "<green>Broken", -1),
                "Discord webhook URL");
        ValidationAssert.assertContains(
                service.validateDiscordSettings(true, true, "https://discord.test/webhook", "<green>Broken", -1),
                "DiscordSRV");
    }

    @Test
    void validatesRequiredMessageKeys() {
        ConfigurationValidationService service = new ConfigurationValidationService(new MiniMessageComponentRenderer());
        PluginMessages messages = new PluginMessages("en", Map.of(
                "reload-success", "Reloaded.",
                "reload-error", "Reload failed."));

        ValidationAssert.assertContains(service.validatePluginMessages(messages), "message key");
    }

    private static final class ValidationAssert {
        private static void assertContains(dev.studio.announcer.domain.validation.ValidationResult result, String... parts) {
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(error -> {
                String lower = error.toLowerCase(java.util.Locale.ROOT);
                for (String part : parts) {
                    if (!lower.contains(part.toLowerCase(java.util.Locale.ROOT))) {
                        return false;
                    }
                }
                return true;
            }), () -> "Expected error containing " + String.join(", ", parts) + " but got " + result.errors());
        }
    }
}
