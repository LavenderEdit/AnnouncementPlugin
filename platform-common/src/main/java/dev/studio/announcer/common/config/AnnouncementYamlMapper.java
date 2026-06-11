package dev.studio.announcer.common.config;

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
import java.util.stream.Collectors;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class AnnouncementYamlMapper {

    private AnnouncementYamlMapper() {
    }

    public static Announcement read(String id, ConfigurationNode node) {
        try {
            Announcement.Builder builder = Announcement.builder(
                            AnnouncementId.of(id),
                            text(node, "name", id))
                    .enabled(bool(node, true, "enabled"))
                    .type(enumValue(AnnouncementType.class, text(node, "type", "GLOBAL")))
                    .channels(channels(node.node("channels").getList(String.class, List.of("CHAT"))))
                    .messages(node.node("messages").getList(String.class, List.of()))
                    .permission(textOrNull(node.node("permission")))
                    .targetServers(set(node.node("target-servers").getList(String.class, List.of())))
                    .targetGroups(set(node.node("target-groups").getList(String.class, List.of())))
                    .priority(node.node("priority").getInt(0))
                    .conditions(node.node("conditions").getList(String.class, List.of()))
                    .metadata(stringMap(node.node("metadata")));
            duration(node.node("interval")).ifPresent(builder::interval);
            String cron = textOrNull(node.node("cron"));
            if (cron != null) {
                builder.cronExpression(cron);
            }
            if (!node.node("sound").virtual()) {
                builder.soundOptions(new SoundOptions(
                        text(node.node("sound"), "key", "minecraft:block.note_block.pling"),
                        (float) node.node("sound", "volume").getDouble(1.0d),
                        (float) node.node("sound", "pitch").getDouble(1.0d)));
            }
            if (!node.node("title").virtual()) {
                builder.titleOptions(new TitleOptions(
                        text(node.node("title"), "title", ""),
                        text(node.node("title"), "subtitle", ""),
                        durationOrDefault(node.node("title", "fade-in"), Duration.ZERO),
                        durationOrDefault(node.node("title", "stay"), Duration.ofSeconds(2)),
                        durationOrDefault(node.node("title", "fade-out"), Duration.ZERO)));
            }
            if (!node.node("actionbar").virtual()) {
                builder.actionBarOptions(new ActionBarOptions(
                        text(node.node("actionbar"), "message", ""),
                        durationOrDefault(node.node("actionbar", "duration"), Duration.ofSeconds(2)),
                        node.node("actionbar", "priority").getInt(node.node("priority").getInt(0)),
                        textOrNull(node.node("actionbar", "permission")),
                        durationOrDefault(node.node("actionbar", "anti-spam-window"), Duration.ZERO)));
            }
            if (!node.node("bossbar").virtual()) {
                builder.bossBarOptions(new BossBarOptions(
                        text(node.node("bossbar"), "title", ""),
                        enumValue(BossBarColor.class, text(node.node("bossbar"), "color", "WHITE")),
                        enumValue(BossBarOverlay.class, text(node.node("bossbar"), "overlay", "PROGRESS")),
                        (float) node.node("bossbar", "progress").getDouble(1.0d),
                        durationOrDefault(node.node("bossbar", "duration"), Duration.ofSeconds(3)),
                        node.node("bossbar", "priority").getInt(node.node("priority").getInt(0)),
                        textOrNull(node.node("bossbar", "permission")),
                        bool(node.node("bossbar"), false, "animated-progress"),
                        bool(node.node("bossbar"), true, "auto-hide")));
            }
            if (!node.node("toast").virtual()) {
                builder.toastOptions(new ToastOptions(
                        bool(node.node("toast"), true, "enabled"),
                        text(node.node("toast"), "title", ""),
                        text(node.node("toast"), "description", ""),
                        text(node.node("toast"), "icon-material", ""),
                        enumValue(ToastFrameType.class, text(node.node("toast"), "frame-type", "TASK")),
                        durationOrDefault(node.node("toast", "duration"), Duration.ofSeconds(3)),
                        bool(node.node("toast"), false, "dynamic-player-head-icon"),
                        node.node("toast", "custom-model-data").virtual()
                                ? null
                                : node.node("toast", "custom-model-data").getInt(),
                        textOrNull(node.node("toast", "permission"))));
            }
            Instant created = instantOrDefault(node.node("created-at"), Instant.now());
            Instant updated = instantOrDefault(node.node("updated-at"), created);
            builder.timestamps(created, updated);
            return builder.build();
        } catch (SerializationException ex) {
            throw new IllegalArgumentException("Invalid announcement YAML for '" + id + "'.", ex);
        }
    }

    public static void write(Announcement announcement, ConfigurationNode node) {
        try {
            node.node("name").set(announcement.name());
            node.node("enabled").set(announcement.enabled());
            node.node("type").set(announcement.type().name());
            node.node("channels").set(announcement.channels().stream().map(Enum::name).sorted().toList());
            node.node("messages").set(announcement.messages());
            setOptional(node.node("permission"), announcement.permission().orElse(null));
            node.node("target-servers").set(announcement.targetServers().stream().sorted().toList());
            node.node("target-groups").set(announcement.targetGroups().stream().sorted().toList());
            announcement.interval().ifPresentOrElse(
                    value -> setString(node.node("interval"), value.toString()),
                    () -> clear(node.node("interval")));
            announcement.cronExpression().ifPresentOrElse(
                    value -> setString(node.node("cron"), value),
                    () -> clear(node.node("cron")));
            node.node("priority").set(announcement.priority());
            announcement.soundOptions().ifPresentOrElse(value -> {
                setString(node.node("sound", "key"), value.key());
                setNumber(node.node("sound", "volume"), value.volume());
                setNumber(node.node("sound", "pitch"), value.pitch());
            }, () -> clear(node.node("sound")));
            announcement.titleOptions().ifPresentOrElse(value -> {
                setString(node.node("title", "title"), value.title());
                setString(node.node("title", "subtitle"), value.subtitle());
                setString(node.node("title", "fade-in"), value.fadeIn().toString());
                setString(node.node("title", "stay"), value.stay().toString());
                setString(node.node("title", "fade-out"), value.fadeOut().toString());
            }, () -> clear(node.node("title")));
            announcement.actionBarOptions().ifPresentOrElse(value -> {
                setString(node.node("actionbar", "message"), value.message());
                setString(node.node("actionbar", "duration"), value.duration().toString());
                setNumber(node.node("actionbar", "priority"), value.priority());
                setOptional(node.node("actionbar", "permission"), value.permissionValue().orElse(null));
                setString(node.node("actionbar", "anti-spam-window"), value.antiSpamWindow().toString());
            }, () -> clear(node.node("actionbar")));
            announcement.bossBarOptions().ifPresentOrElse(value -> {
                setString(node.node("bossbar", "title"), value.title());
                setString(node.node("bossbar", "color"), value.color().name());
                setString(node.node("bossbar", "overlay"), value.overlay().name());
                setNumber(node.node("bossbar", "progress"), value.progress());
                setString(node.node("bossbar", "duration"), value.duration().toString());
                setNumber(node.node("bossbar", "priority"), value.priority());
                setOptional(node.node("bossbar", "permission"), value.permissionValue().orElse(null));
                setBoolean(node.node("bossbar", "animated-progress"), value.animatedProgress());
                setBoolean(node.node("bossbar", "auto-hide"), value.autoHide());
            }, () -> clear(node.node("bossbar")));
            announcement.toastOptions().ifPresentOrElse(value -> {
                setBoolean(node.node("toast", "enabled"), value.enabled());
                setString(node.node("toast", "title"), value.title());
                setString(node.node("toast", "description"), value.description());
                setString(node.node("toast", "icon-material"), value.iconMaterial());
                setString(node.node("toast", "frame-type"), value.frameType().name());
                setString(node.node("toast", "duration"), value.duration().toString());
                setBoolean(node.node("toast", "dynamic-player-head-icon"), value.dynamicPlayerHeadIcon());
                if (value.customModelData() == null) {
                    clear(node.node("toast", "custom-model-data"));
                } else {
                    setNumber(node.node("toast", "custom-model-data"), value.customModelData());
                }
                setOptional(node.node("toast", "permission"), value.permissionValue().orElse(null));
            }, () -> clear(node.node("toast")));
            node.node("conditions").set(announcement.conditions());
            node.node("metadata").set(announcement.metadata());
            node.node("created-at").set(announcement.createdAt().toString());
            node.node("updated-at").set(announcement.updatedAt().toString());
        } catch (SerializationException ex) {
            throw new IllegalArgumentException("Could not write announcement YAML for '" + announcement.id().value() + "'.", ex);
        }
    }

    private static EnumSet<AnnouncementChannel> channels(List<String> names) {
        EnumSet<AnnouncementChannel> channels = EnumSet.noneOf(AnnouncementChannel.class);
        for (String name : names) {
            channels.add(enumValue(AnnouncementChannel.class, name));
        }
        return channels.isEmpty() ? EnumSet.of(AnnouncementChannel.CHAT) : channels;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String name) {
        return Enum.valueOf(type, name.trim().replace('-', '_').toUpperCase(java.util.Locale.ROOT));
    }

    private static java.util.Optional<Duration> duration(ConfigurationNode node) {
        String raw = textOrNull(node);
        if (raw == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Duration.parse(raw));
    }

    private static Duration durationOrDefault(ConfigurationNode node, Duration fallback) {
        return duration(node).orElse(fallback);
    }

    private static Instant instantOrDefault(ConfigurationNode node, Instant fallback) {
        String raw = textOrNull(node);
        return raw == null ? fallback : Instant.parse(raw);
    }

    private static String text(ConfigurationNode node, String key, String fallback) {
        return node.node(key).getString(fallback);
    }

    private static String textOrNull(ConfigurationNode node) {
        String value = node.getString();
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean bool(ConfigurationNode node, boolean fallback, String key) {
        return node.node(key).getBoolean(fallback);
    }

    private static Set<String> set(List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, String> stringMap(ConfigurationNode node) {
        return node.childrenMap().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                entry -> String.valueOf(entry.getKey()),
                entry -> entry.getValue().getString("")));
    }

    private static void setString(ConfigurationNode node, String value) {
        try {
            node.set(value);
        } catch (SerializationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static void setOptional(ConfigurationNode node, String value) {
        try {
            node.set(value);
        } catch (SerializationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static void setNumber(ConfigurationNode node, Number value) {
        try {
            node.set(value);
        } catch (SerializationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static void setBoolean(ConfigurationNode node, boolean value) {
        try {
            node.set(value);
        } catch (SerializationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static void clear(ConfigurationNode node) {
        try {
            node.set(null);
        } catch (SerializationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
