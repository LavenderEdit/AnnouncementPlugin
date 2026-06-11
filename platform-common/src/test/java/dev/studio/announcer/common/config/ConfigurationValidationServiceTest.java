package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.EnumSet;
import java.util.List;
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
}
