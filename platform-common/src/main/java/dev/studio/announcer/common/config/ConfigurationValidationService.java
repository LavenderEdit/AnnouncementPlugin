package dev.studio.announcer.common.config;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.common.message.PluginMessages;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementValidator;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ConfigurationValidationService {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern SAFE_PERMISSION = Pattern.compile("[A-Za-z0-9*_.-]+");
    private static final Pattern SAFE_KEY = Pattern.compile("([A-Za-z0-9_.-]+:)?[A-Za-z0-9_./-]+");
    private static final Pattern SAFE_CRON_FIELD = Pattern.compile("[A-Za-z0-9*/?,#LW-]+");
    private static final Set<String> REQUIRED_MESSAGE_KEYS = Set.of(
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

    private final AnnouncementValidator announcementValidator = new AnnouncementValidator();
    private final MessageRenderer<?> renderer;

    public ConfigurationValidationService(MessageRenderer<?> renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public ValidationResult validateAnnouncements(Collection<Announcement> announcements) {
        List<String> errors = new ArrayList<>();
        if (announcements == null) {
            return ValidationResult.invalid("Announcements cannot be null.");
        }
        for (Announcement announcement : announcements) {
            ValidationResult domain = announcementValidator.validate(announcement);
            domain.errors().forEach(error -> errors.add(announcement.id().value() + ": " + error));
            for (String message : announcement.messages()) {
                ValidationResult rendered = renderer.validate(message);
                rendered.errors().forEach(error -> errors.add(announcement.id().value() + ": " + error));
            }
            announcement.titleOptions().ifPresent(options -> {
                collect(errors, announcement, renderer.validate(options.title()));
                collect(errors, announcement, renderer.validate(options.subtitle()));
            });
            announcement.actionBarOptions().ifPresent(options -> collect(errors, announcement, renderer.validate(options.message())));
            announcement.bossBarOptions().ifPresent(options -> collect(errors, announcement, renderer.validate(options.title())));
            announcement.toastOptions().ifPresent(options -> {
                collect(errors, announcement, renderer.validate(options.title()));
                collect(errors, announcement, renderer.validate(options.description()));
            });
            announcement.cronExpression().ifPresent(cron -> {
                String[] fields = cron.trim().split("\\s+");
                if (fields.length < 5 || fields.length > 6) {
                    errors.add(announcement.id().value() + ": cron expression must contain 5 or 6 fields.");
                } else {
                    for (String field : fields) {
                        if (!SAFE_CRON_FIELD.matcher(field).matches()) {
                            errors.add(announcement.id().value() + ": cron expression contains invalid field '" + field + "'.");
                        }
                    }
                }
            });
            collectPermission(errors, announcement, "permission", announcement.permission().orElse(null));
            announcement.targetServers().forEach(value -> collectIdentifier(errors, announcement, "target server", value));
            announcement.targetGroups().forEach(value -> collectIdentifier(errors, announcement, "target group", value));
            announcement.soundOptions().ifPresent(options -> collectKey(errors, announcement, "sound key", options.key()));
            announcement.actionBarOptions().ifPresent(options ->
                    collectPermission(errors, announcement, "actionbar permission", options.permissionValue().orElse(null)));
            announcement.bossBarOptions().ifPresent(options ->
                    collectPermission(errors, announcement, "bossbar permission", options.permissionValue().orElse(null)));
            announcement.toastOptions().ifPresent(options -> {
                collectKey(errors, announcement, "toast material", options.iconMaterial());
                collectPermission(errors, announcement, "toast permission", options.permissionValue().orElse(null));
            });
        }
        return ValidationResult.fromErrors(errors);
    }

    public ValidationResult validateRedisSettings(
            boolean enabled,
            String uri,
            String channel,
            long reconnectDelaySeconds) {
        if (!enabled) {
            return ValidationResult.ok();
        }
        List<String> errors = new ArrayList<>();
        URI parsed = parseUri(uri, "Redis URI", errors);
        if (parsed != null && !List.of("redis", "rediss").contains(parsed.getScheme())) {
            errors.add("Redis URI must use redis:// or rediss://.");
        }
        if (channel == null || channel.isBlank()) {
            errors.add("Redis channel cannot be blank when Redis is enabled.");
        }
        if (reconnectDelaySeconds <= 0) {
            errors.add("Redis reconnect-delay-seconds must be positive.");
        }
        return ValidationResult.fromErrors(errors);
    }

    public ValidationResult validateDiscordSettings(
            boolean discordEnabled,
            boolean webhookEnabled,
            String webhookUrl,
            String minecraftFormat,
            long cooldownSeconds) {
        if (!discordEnabled) {
            return ValidationResult.ok();
        }
        List<String> errors = new ArrayList<>();
        if (webhookEnabled) {
            URI parsed = parseUri(webhookUrl, "Discord webhook URL", errors);
            if (parsed != null && (!List.of("http", "https").contains(parsed.getScheme())
                    || parsed.getHost() == null
                    || parsed.getHost().isBlank())) {
                errors.add("Discord webhook URL must be an HTTP or HTTPS URL with a host.");
            }
        }
        ValidationResult format = renderer.validate(minecraftFormat == null ? "" : minecraftFormat);
        format.errors().forEach(error -> errors.add("DiscordSRV minecraft-format: " + error));
        if (cooldownSeconds < 0) {
            errors.add("DiscordSRV cooldown-seconds cannot be negative.");
        }
        return ValidationResult.fromErrors(errors);
    }

    public ValidationResult validatePluginMessages(PluginMessages messages) {
        if (messages == null) {
            return ValidationResult.invalid("Plugin messages cannot be null.");
        }
        List<String> errors = new ArrayList<>();
        for (String key : REQUIRED_MESSAGE_KEYS) {
            if (messages.message(key, "").isBlank()) {
                errors.add("Missing required message key: " + key);
            }
        }
        return ValidationResult.fromErrors(errors);
    }

    private void collect(List<String> errors, Announcement announcement, ValidationResult result) {
        result.errors().forEach(error -> errors.add(announcement.id().value() + ": " + error));
    }

    private URI parseUri(String raw, String label, List<String> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add(label + " cannot be blank when the integration is enabled.");
            return null;
        }
        try {
            URI parsed = new URI(raw.trim());
            if (parsed.getScheme() == null || parsed.getScheme().isBlank()) {
                errors.add(label + " must include a URI scheme.");
                return null;
            }
            return parsed;
        } catch (URISyntaxException ex) {
            errors.add(label + " is invalid: " + ex.getMessage());
            return null;
        }
    }

    private void collectPermission(List<String> errors, Announcement announcement, String label, String value) {
        if (value != null && !value.isBlank() && !SAFE_PERMISSION.matcher(value).matches()) {
            errors.add(announcement.id().value() + ": " + label + " contains invalid characters.");
        }
    }

    private void collectIdentifier(List<String> errors, Announcement announcement, String label, String value) {
        if (value != null && !value.isBlank() && !SAFE_IDENTIFIER.matcher(value).matches()) {
            errors.add(announcement.id().value() + ": " + label + " contains invalid characters.");
        }
    }

    private void collectKey(List<String> errors, Announcement announcement, String label, String value) {
        if (value != null && !value.isBlank() && !SAFE_KEY.matcher(value).matches()) {
            errors.add(announcement.id().value() + ": " + label + " contains invalid characters.");
        }
    }
}
