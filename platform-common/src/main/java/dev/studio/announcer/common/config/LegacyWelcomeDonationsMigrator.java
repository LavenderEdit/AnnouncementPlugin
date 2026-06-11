package dev.studio.announcer.common.config;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import dev.studio.announcer.domain.announcement.option.SoundOptions;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class LegacyWelcomeDonationsMigrator {

    public List<Announcement> migrate(Path legacyConfig) {
        try {
            ConfigurationNode root = YamlConfigurationLoader.builder().path(legacyConfig).build().load();
            List<Announcement> migrated = new ArrayList<>();
            migrateWelcome(root, migrated);
            migrateDonation(root, migrated);
            migrateRank(root, migrated);
            return List.copyOf(migrated);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not migrate legacy WelcomeDonations config.", ex);
        }
    }

    private void migrateWelcome(ConfigurationNode root, List<Announcement> migrated) {
        ConfigurationNode welcome = root.node("welcome");
        if (!welcome.node("enabled").getBoolean(true)) {
            return;
        }
        ConfigurationNode firstJoin = welcome.node("first-join");
        if (firstJoin.node("enabled").getBoolean(false)) {
            migrated.add(Announcement.builder(AnnouncementId.of("legacy_first_join"), "Legacy First Join")
                    .type(AnnouncementType.EVENT_JOIN)
                    .channels(EnumSet.of(AnnouncementChannel.CHAT))
                    .messages(List.of(firstJoin.node("chat").getString("<green>Welcome %player%</green>")))
                    .build());
        }
        String rejoin = welcome.node("rejoin", "chat").getString();
        if (rejoin != null && !rejoin.isBlank()) {
            migrated.add(Announcement.builder(AnnouncementId.of("legacy_rejoin"), "Legacy Rejoin")
                    .type(AnnouncementType.EVENT_JOIN)
                    .channels(EnumSet.of(AnnouncementChannel.CHAT))
                    .messages(List.of(rejoin))
                    .build());
        }
    }

    private void migrateDonation(ConfigurationNode root, List<Announcement> migrated) {
        ConfigurationNode donations = root.node("donations");
        if (!donations.node("enabled").getBoolean(false)) {
            return;
        }
        Announcement.Builder builder = Announcement.builder(AnnouncementId.of("legacy_donation"), "Legacy Donation")
                .type(AnnouncementType.GLOBAL)
                .channels(EnumSet.of(AnnouncementChannel.CHAT, AnnouncementChannel.SOUND))
                .messages(List.of(donations.node("format").getString("<gold>%player% donated %item%</gold>")));
        String sound = donations.node("sound").getString();
        if (sound != null && !sound.isBlank()) {
            builder.soundOptions(new SoundOptions(sound, 1.0f, 1.0f));
        }
        migrated.add(builder.build());
    }

    private void migrateRank(ConfigurationNode root, List<Announcement> migrated) {
        ConfigurationNode ranks = root.node("ranks");
        if (!ranks.node("enabled").getBoolean(false)) {
            return;
        }
        Announcement.Builder builder = Announcement.builder(AnnouncementId.of("legacy_rank"), "Legacy Rank")
                .type(AnnouncementType.RANK_BASED)
                .channels(EnumSet.of(AnnouncementChannel.CHAT, AnnouncementChannel.SOUND))
                .messages(List.of(ranks.node("format").getString("<gold>%player% bought %rank%</gold>")));
        String sound = ranks.node("sound").getString();
        if (sound != null && !sound.isBlank()) {
            builder.soundOptions(new SoundOptions(sound, 1.0f, 1.0f));
        }
        migrated.add(builder.build());
    }
}
