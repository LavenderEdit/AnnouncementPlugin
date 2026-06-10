package dev.studio.announcer.domain.announcement;

import dev.studio.announcer.domain.announcement.option.ActionBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarOptions;
import dev.studio.announcer.domain.announcement.option.SoundOptions;
import dev.studio.announcer.domain.announcement.option.TitleOptions;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record Announcement(
        AnnouncementId id,
        String name,
        boolean enabled,
        AnnouncementType type,
        Set<AnnouncementChannel> channels,
        List<String> messages,
        Optional<String> permission,
        Set<String> targetServers,
        Set<String> targetGroups,
        Optional<Duration> interval,
        Optional<String> cronExpression,
        int priority,
        Optional<SoundOptions> soundOptions,
        Optional<ToastOptions> toastOptions,
        Optional<BossBarOptions> bossBarOptions,
        Optional<ActionBarOptions> actionBarOptions,
        Optional<TitleOptions> titleOptions,
        List<String> conditions,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt) {

    public Announcement {
        id = Objects.requireNonNull(id, "id");
        name = requireNonBlank(name, "Announcement name");
        type = type == null ? AnnouncementType.GLOBAL : type;
        channels = copyChannels(channels);
        messages = copyList(messages);
        permission = normalizeOptional(permission);
        targetServers = copyStrings(targetServers);
        targetGroups = copyStrings(targetGroups);
        interval = interval == null ? Optional.empty() : interval;
        interval.ifPresent(value -> {
            if (value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException("Announcement interval must be positive.");
            }
        });
        cronExpression = normalizeOptional(cronExpression);
        if (priority < 0) {
            throw new IllegalArgumentException("Announcement priority cannot be negative.");
        }
        soundOptions = soundOptions == null ? Optional.empty() : soundOptions;
        toastOptions = toastOptions == null ? Optional.empty() : toastOptions;
        bossBarOptions = bossBarOptions == null ? Optional.empty() : bossBarOptions;
        actionBarOptions = actionBarOptions == null ? Optional.empty() : actionBarOptions;
        titleOptions = titleOptions == null ? Optional.empty() : titleOptions;
        conditions = copyList(conditions);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Announcement updatedAt cannot be before createdAt.");
        }
    }

    public static Builder builder(AnnouncementId id, String name) {
        return new Builder(id, name);
    }

    public Announcement withEnabled(boolean enabled) {
        return new Announcement(
                id,
                name,
                enabled,
                type,
                channels,
                messages,
                permission,
                targetServers,
                targetGroups,
                interval,
                cronExpression,
                priority,
                soundOptions,
                toastOptions,
                bossBarOptions,
                actionBarOptions,
                titleOptions,
                conditions,
                metadata,
                createdAt,
                Instant.now());
    }

    private static Set<AnnouncementChannel> copyChannels(Set<AnnouncementChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("Announcement requires at least one channel.");
        }
        return Set.copyOf(channels);
    }

    private static Set<String> copyStrings(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList());
    }

    private static List<String> copyList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return List.copyOf(values.stream()
                .filter(Objects::nonNull)
                .toList());
    }

    private static Optional<String> normalizeOptional(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return normalize(value.orElse(null));
    }

    private static Optional<String> normalize(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank.");
        }
        return value.trim();
    }

    public static final class Builder {
        private final AnnouncementId id;
        private final String name;
        private boolean enabled = true;
        private AnnouncementType type = AnnouncementType.GLOBAL;
        private Set<AnnouncementChannel> channels = EnumSet.of(AnnouncementChannel.CHAT);
        private List<String> messages = List.of();
        private Optional<String> permission = Optional.empty();
        private Set<String> targetServers = Set.of();
        private Set<String> targetGroups = Set.of();
        private Optional<Duration> interval = Optional.empty();
        private Optional<String> cronExpression = Optional.empty();
        private int priority;
        private Optional<SoundOptions> soundOptions = Optional.empty();
        private Optional<ToastOptions> toastOptions = Optional.empty();
        private Optional<BossBarOptions> bossBarOptions = Optional.empty();
        private Optional<ActionBarOptions> actionBarOptions = Optional.empty();
        private Optional<TitleOptions> titleOptions = Optional.empty();
        private List<String> conditions = List.of();
        private Map<String, String> metadata = Map.of();
        private Instant createdAt = Instant.now();
        private Instant updatedAt = createdAt;

        private Builder(AnnouncementId id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder type(AnnouncementType type) {
            this.type = type;
            return this;
        }

        public Builder channels(Set<AnnouncementChannel> channels) {
            this.channels = channels;
            return this;
        }

        public Builder messages(List<String> messages) {
            this.messages = messages;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = normalize(permission);
            return this;
        }

        public Builder targetServers(Set<String> targetServers) {
            this.targetServers = targetServers;
            return this;
        }

        public Builder targetGroups(Set<String> targetGroups) {
            this.targetGroups = targetGroups;
            return this;
        }

        public Builder interval(Duration interval) {
            this.interval = Optional.ofNullable(interval);
            return this;
        }

        public Builder cronExpression(String cronExpression) {
            this.cronExpression = normalize(cronExpression);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder soundOptions(SoundOptions soundOptions) {
            this.soundOptions = Optional.ofNullable(soundOptions);
            return this;
        }

        public Builder toastOptions(ToastOptions toastOptions) {
            this.toastOptions = Optional.ofNullable(toastOptions);
            return this;
        }

        public Builder bossBarOptions(BossBarOptions bossBarOptions) {
            this.bossBarOptions = Optional.ofNullable(bossBarOptions);
            return this;
        }

        public Builder actionBarOptions(ActionBarOptions actionBarOptions) {
            this.actionBarOptions = Optional.ofNullable(actionBarOptions);
            return this;
        }

        public Builder titleOptions(TitleOptions titleOptions) {
            this.titleOptions = Optional.ofNullable(titleOptions);
            return this;
        }

        public Builder conditions(List<String> conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder timestamps(Instant createdAt, Instant updatedAt) {
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            return this;
        }

        public Announcement build() {
            return new Announcement(
                    id,
                    name,
                    enabled,
                    type,
                    channels,
                    messages,
                    permission,
                    targetServers,
                    targetGroups,
                    interval,
                    cronExpression,
                    priority,
                    soundOptions,
                    toastOptions,
                    bossBarOptions,
                    actionBarOptions,
                    titleOptions,
                    conditions,
                    metadata,
                    createdAt,
                    updatedAt);
        }
    }
}
