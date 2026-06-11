package dev.studio.announcer.application.command;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.PlatformStatusService;
import dev.studio.announcer.application.usecase.MigrateLegacyConfigurationUseCase;
import dev.studio.announcer.application.usecase.ReloadConfigurationUseCase;
import dev.studio.announcer.application.usecase.AnnouncementNotFoundException;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AnnouncerCommandService {
    private final AnnouncementRepository repository;
    private final AnnouncementDispatcher dispatcher;
    private final PlatformStatusService statusService;
    private final ReloadConfigurationUseCase reloadConfigurationUseCase;
    private final MigrateLegacyConfigurationUseCase migrateLegacyConfigurationUseCase;

    public AnnouncerCommandService(
            AnnouncementRepository repository,
            AnnouncementDispatcher dispatcher,
            PlatformStatusService statusService) {
        this(
                repository,
                dispatcher,
                statusService,
                new ReloadConfigurationUseCase(() -> ConfigurationReloadResult.success("Reload completed.")),
                new MigrateLegacyConfigurationUseCase(() -> ConfigurationReloadResult.failure(
                        "Legacy migration service is not configured.",
                        List.of("No platform migration service was provided."))));
    }

    public AnnouncerCommandService(
            AnnouncementRepository repository,
            AnnouncementDispatcher dispatcher,
            PlatformStatusService statusService,
            ReloadConfigurationUseCase reloadConfigurationUseCase) {
        this(
                repository,
                dispatcher,
                statusService,
                reloadConfigurationUseCase,
                new MigrateLegacyConfigurationUseCase(() -> ConfigurationReloadResult.failure(
                        "Legacy migration service is not configured.",
                        List.of("No platform migration service was provided."))));
    }

    public AnnouncerCommandService(
            AnnouncementRepository repository,
            AnnouncementDispatcher dispatcher,
            PlatformStatusService statusService,
            ReloadConfigurationUseCase reloadConfigurationUseCase,
            MigrateLegacyConfigurationUseCase migrateLegacyConfigurationUseCase) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.reloadConfigurationUseCase = Objects.requireNonNull(reloadConfigurationUseCase, "reloadConfigurationUseCase");
        this.migrateLegacyConfigurationUseCase = Objects.requireNonNull(
                migrateLegacyConfigurationUseCase,
                "migrateLegacyConfigurationUseCase");
    }

    public CommandOutcome handle(CommandRequest request) {
        if (request == null || request.arguments().isEmpty()) {
            return help();
        }
        String subcommand = request.argument(0).toLowerCase(Locale.ROOT);
        try {
            return switch (subcommand) {
                case "version" -> CommandOutcome.success("AdvancedAnnouncer running on "
                        + statusService.platformName() + " " + statusService.platformVersion() + ".");
                case "debug" -> debug();
                case "list" -> list();
                case "create" -> create(request);
                case "delete" -> delete(request);
                case "toggle" -> toggle(request);
                case "send" -> send(request);
                case "preview" -> preview(request);
                case "reload" -> reload();
                case "migrate" -> migrate();
                case "redis" -> diagnostic(request, "redis", "Redis is disabled or not configured in this phase.");
                case "discord" -> diagnostic(request, "discord", "Discord is disabled or not configured in this phase.");
                default -> help();
            };
        } catch (IllegalArgumentException | AnnouncementNotFoundException ex) {
            return CommandOutcome.error(ex.getMessage());
        }
    }

    public List<String> suggestions(String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return List.of("debug", "delete", "discord", "editor", "create", "list", "migrate", "preview", "redis", "reload", "send", "toggle", "version")
                .stream()
                .filter(value -> value.startsWith(lower))
                .toList();
    }

    private CommandOutcome create(CommandRequest request) {
        requireArgs(request, 2, "Usage: /announcer create <id>");
        AnnouncementId id = AnnouncementId.of(request.argument(1));
        if (repository.findById(id).isPresent()) {
            return CommandOutcome.error("Announcement '" + id.value() + "' already exists.");
        }
        Announcement announcement = Announcement.builder(id, id.value())
                .type(AnnouncementType.GLOBAL)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>Announcement " + id.value() + "</green>"))
                .build();
        repository.save(announcement);
        return CommandOutcome.success("Created announcement '" + id.value() + "'.");
    }

    private CommandOutcome delete(CommandRequest request) {
        requireArgs(request, 2, "Usage: /announcer delete <id>");
        AnnouncementId id = AnnouncementId.of(request.argument(1));
        if (!repository.deleteById(id)) {
            return CommandOutcome.error("Announcement not found: " + id.value());
        }
        return CommandOutcome.success("Deleted announcement '" + id.value() + "'.");
    }

    private CommandOutcome toggle(CommandRequest request) {
        requireArgs(request, 2, "Usage: /announcer toggle <id>");
        AnnouncementId id = AnnouncementId.of(request.argument(1));
        Announcement current = repository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        Announcement updated = current.withEnabled(!current.enabled());
        repository.save(updated);
        return CommandOutcome.success("Announcement '" + id.value() + "' "
                + (updated.enabled() ? "enabled" : "disabled") + ".");
    }

    private CommandOutcome send(CommandRequest request) {
        requireArgs(request, 2, "Usage: /announcer send <id>");
        AnnouncementId id = AnnouncementId.of(request.argument(1));
        Announcement announcement = repository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        DeliverySummary summary = dispatcher.broadcast(announcement);
        return CommandOutcome.success("Sent '" + id.value() + "' to " + summary.delivered()
                + " audience(s). Skipped=" + summary.skippedByPermission() + ", invalid=" + summary.invalid() + ".");
    }

    private CommandOutcome preview(CommandRequest request) {
        requireArgs(request, 2, "Usage: /announcer preview <id>");
        if (!request.player()) {
            return CommandOutcome.error("Preview can only be sent to an in-game player.");
        }
        AnnouncementId id = AnnouncementId.of(request.argument(1));
        Announcement announcement = repository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        DeliverySummary summary = dispatcher.preview(announcement, request.senderId());
        return CommandOutcome.success("Previewed '" + id.value() + "' to " + summary.delivered() + " audience(s).");
    }

    private CommandOutcome list() {
        List<String> ids = repository.findAll().stream()
                .sorted(Comparator.comparing(announcement -> announcement.id().value()))
                .map(announcement -> announcement.id().value() + (announcement.enabled() ? "" : " (disabled)"))
                .toList();
        if (ids.isEmpty()) {
            return CommandOutcome.success("No announcements loaded.");
        }
        return CommandOutcome.success("Announcements: " + String.join(", ", ids));
    }

    private CommandOutcome debug() {
        return CommandOutcome.success(List.of(
                "Platform=" + statusService.platformName() + " " + statusService.platformVersion(),
                "Folia=" + (statusService.foliaDetected() ? "detected" : "not detected"),
                "PlaceholderAPI=" + (statusService.placeholderApiAvailable() ? "available" : "absent"),
                "Redis=" + (statusService.redisEnabled() ? "enabled" : "disabled"),
                "Discord=" + (statusService.discordEnabled() ? "enabled" : "disabled"),
                "Announcements=" + repository.findAll().size()));
    }

    private CommandOutcome reload() {
        ConfigurationReloadResult result = reloadConfigurationUseCase.reload();
        if (result.success()) {
            return CommandOutcome.success(result.message());
        }
        java.util.List<String> messages = new java.util.ArrayList<>();
        messages.add(result.message());
        messages.addAll(result.errors());
        return CommandOutcome.error(messages);
    }

    private CommandOutcome migrate() {
        ConfigurationReloadResult result = migrateLegacyConfigurationUseCase.migrate();
        if (result.success()) {
            return CommandOutcome.success(result.message());
        }
        java.util.List<String> messages = new java.util.ArrayList<>();
        messages.add(result.message());
        messages.addAll(result.errors());
        return CommandOutcome.error(messages);
    }

    private CommandOutcome diagnostic(CommandRequest request, String name, String disabledMessage) {
        if (request.arguments().size() >= 2 && "test".equalsIgnoreCase(request.argument(1))) {
            return CommandOutcome.success(disabledMessage);
        }
        return CommandOutcome.error("Usage: /announcer " + name + " test");
    }

    private CommandOutcome help() {
        return CommandOutcome.success(List.of(
                "AdvancedAnnouncer commands:",
                "/announcer list",
                "/announcer create <id>",
                "/announcer delete <id>",
                "/announcer toggle <id>",
                "/announcer send <id>",
                "/announcer preview <id>",
                "/announcer editor",
                "/announcer reload",
                "/announcer migrate",
                "/announcer debug",
                "/announcer version"));
    }

    private void requireArgs(CommandRequest request, int count, String usage) {
        if (request.arguments().size() < count) {
            throw new IllegalArgumentException(usage);
        }
    }
}
