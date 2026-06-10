package dev.studio.announcer.domain.announcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.domain.announcement.option.ActionBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarColor;
import dev.studio.announcer.domain.announcement.option.BossBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarOverlay;
import dev.studio.announcer.domain.announcement.option.SoundOptions;
import dev.studio.announcer.domain.announcement.option.TitleOptions;
import dev.studio.announcer.domain.announcement.option.ToastFrameType;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementTest {

    @Test
    void createsEnabledAnnouncementWithMultipleChannels() {
        Announcement announcement = Announcement.builder(AnnouncementId.of("global_xp"), "Global XP")
                .type(AnnouncementType.GLOBAL)
                .channels(EnumSet.of(
                        AnnouncementChannel.CHAT,
                        AnnouncementChannel.TOAST,
                        AnnouncementChannel.SOUND))
                .messages(List.of("<gradient:gold:yellow>Double XP is live</gradient>"))
                .permission("announcer.receive.alert")
                .priority(25)
                .soundOptions(new SoundOptions("ui.toast.challenge_complete", 1.0f, 1.0f))
                .toastOptions(new ToastOptions(
                        true,
                        "<gold>Double XP</gold>",
                        "<yellow>Earn twice as much experience.</yellow>",
                        "EXPERIENCE_BOTTLE",
                        ToastFrameType.CHALLENGE,
                        Duration.ofSeconds(4),
                        false,
                        null,
                        "announcer.receive.alert"))
                .build();

        assertEquals(AnnouncementId.of("global_xp"), announcement.id());
        assertTrue(announcement.enabled());
        assertTrue(announcement.channels().contains(AnnouncementChannel.CHAT));
        assertTrue(announcement.channels().contains(AnnouncementChannel.TOAST));
        assertEquals(25, announcement.priority());
        assertEquals("announcer.receive.alert", announcement.permission().orElseThrow());
        assertEquals("ui.toast.challenge_complete", announcement.soundOptions().orElseThrow().key());
    }

    @Test
    void rejectsBlankAnnouncementId() {
        assertThrows(IllegalArgumentException.class, () -> AnnouncementId.of(" "));
    }

    @Test
    void rejectsAnnouncementNameThatIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Announcement.builder(AnnouncementId.of("welcome"), " ")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Welcome</green>"))
                .build());
    }

    @Test
    void rejectsAnnouncementWithoutChannels() {
        assertThrows(IllegalArgumentException.class, () -> Announcement.builder(AnnouncementId.of("empty_channels"), "Empty")
                .channels(EnumSet.noneOf(AnnouncementChannel.class))
                .messages(List.of("<green>Welcome</green>"))
                .build());
    }

    @Test
    void rejectsNegativePriority() {
        assertThrows(IllegalArgumentException.class, () -> Announcement.builder(AnnouncementId.of("bad_priority"), "Priority")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Welcome</green>"))
                .priority(-1)
                .build());
    }

    @Test
    void validatesMinimumVisualOptions() {
        ActionBarOptions actionBar = new ActionBarOptions(
                "<aqua>Queue message</aqua>",
                Duration.ofSeconds(3),
                10,
                null,
                Duration.ofMillis(250));
        BossBarOptions bossBar = new BossBarOptions(
                "<red>Boss warning</red>",
                BossBarColor.RED,
                BossBarOverlay.PROGRESS,
                0.75f,
                Duration.ofSeconds(5),
                20,
                null,
                true,
                true);
        TitleOptions title = new TitleOptions(
                "<gold>Title</gold>",
                "<yellow>Subtitle</yellow>",
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofMillis(500));

        assertEquals(10, actionBar.priority());
        assertEquals(0.75f, bossBar.progress());
        assertTrue(bossBar.autoHide());
        assertFalse(title.title().isBlank());
    }
}
