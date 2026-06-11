package dev.studio.announcer.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import dev.studio.announcer.domain.announcement.option.ActionBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarColor;
import dev.studio.announcer.domain.announcement.option.BossBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarOverlay;
import dev.studio.announcer.domain.announcement.option.SoundOptions;
import dev.studio.announcer.domain.announcement.option.TitleOptions;
import dev.studio.announcer.domain.announcement.option.ToastFrameType;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

class AnnouncementYamlMapperTest {

    @Test
    void roundTripsAnnouncementWithVisualOptions() throws Exception {
        Announcement original = Announcement.builder(AnnouncementId.of("double_xp"), "Double XP")
                .enabled(true)
                .type(AnnouncementType.SCHEDULED)
                .channels(EnumSet.of(
                        AnnouncementChannel.CHAT,
                        AnnouncementChannel.TITLE,
                        AnnouncementChannel.ACTIONBAR,
                        AnnouncementChannel.BOSSBAR,
                        AnnouncementChannel.TOAST,
                        AnnouncementChannel.SOUND))
                .messages(List.of("<green>Hello {player_name}</green>", "<yellow>Enjoy!</yellow>"))
                .permission("announcer.receive.vip")
                .targetServers(Set.of("lobby", "survival"))
                .targetGroups(Set.of("vip"))
                .interval(Duration.ofSeconds(120))
                .cronExpression("0 */5 * * *")
                .priority(7)
                .soundOptions(new SoundOptions("minecraft:ui.toast.challenge_complete", 1.0f, 1.2f))
                .titleOptions(new TitleOptions("<gold>XP</gold>", "<yellow>Live</yellow>",
                        Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(250)))
                .actionBarOptions(new ActionBarOptions("<aqua>Boost active</aqua>",
                        Duration.ofSeconds(4), 8, "announcer.receive.vip", Duration.ofSeconds(3)))
                .bossBarOptions(new BossBarOptions("<gold>Double XP</gold>",
                        BossBarColor.YELLOW, BossBarOverlay.NOTCHED_10, 0.85f,
                        Duration.ofSeconds(6), 9, "announcer.receive.vip", true, true))
                .toastOptions(new ToastOptions(true, "<gold>Double XP</gold>", "<yellow>Started</yellow>",
                        "EXPERIENCE_BOTTLE", ToastFrameType.CHALLENGE, Duration.ofSeconds(4),
                        false, 12, "announcer.receive.vip"))
                .conditions(List.of("online>=1"))
                .metadata(Map.of("source", "test"))
                .timestamps(Instant.parse("2026-06-10T12:00:00Z"), Instant.parse("2026-06-10T12:01:00Z"))
                .build();
        ConfigurationNode node = YamlConfigurationLoader.builder()
                .buildAndLoadString("")
                .node("announcements", "double_xp");

        AnnouncementYamlMapper.write(original, node);
        Announcement loaded = AnnouncementYamlMapper.read("double_xp", node);

        assertEquals(original.id(), loaded.id());
        assertEquals(original.name(), loaded.name());
        assertEquals(original.channels(), loaded.channels());
        assertEquals(original.messages(), loaded.messages());
        assertEquals(original.permission(), loaded.permission());
        assertEquals(original.interval(), loaded.interval());
        assertEquals(original.cronExpression(), loaded.cronExpression());
        assertEquals(original.priority(), loaded.priority());
        assertEquals(original.soundOptions(), loaded.soundOptions());
        assertEquals(original.titleOptions(), loaded.titleOptions());
        assertEquals(original.actionBarOptions(), loaded.actionBarOptions());
        assertEquals(original.bossBarOptions(), loaded.bossBarOptions());
        assertEquals(original.toastOptions(), loaded.toastOptions());
        assertEquals(original.conditions(), loaded.conditions());
        assertEquals(original.metadata(), loaded.metadata());
        assertTrue(loaded.enabled());
    }
}
