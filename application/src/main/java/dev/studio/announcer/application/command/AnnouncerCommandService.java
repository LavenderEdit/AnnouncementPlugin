package dev.studio.announcer.application.command;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.discord.DiscordOutboundMessage;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.DiscordBridgeService;
import dev.studio.announcer.api.service.NetworkBroadcastService;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AnnouncerCommandService {
    private final AnnouncementRepository repository;
    private final AnnouncementDispatcher dispatcher;
    private final PlatformStatusService statusService;
    private final ReloadConfigurationUseCase reloadConfigurationUseCase;
    private final MigrateLegacyConfigurationUseCase migrateLegacyConfigurationUseCase;
    private final NetworkBroadcastService networkBroadcastService;
    private final DiscordBridgeService discordBridgeService;

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
                        List.of("No platform migration service was provided."))),
                new DisabledNetworkBroadcastService(),
                new DisabledDiscordBridgeService());
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
                        List.of("No platform migration service was provided."))),
                new DisabledNetworkBroadcastService(),
                new DisabledDiscordBridgeService());
    }

    public AnnouncerCommandService(
            AnnouncementRepository repository,
            AnnouncementDispatcher dispatcher,
            PlatformStatusService statusService,
            ReloadConfigurationUseCase reloadConfigurationUseCase,
            MigrateLegacyConfigurationUseCase migrateLegacyConfigurationUseCase) {
        this(
                repository,
                dispatcher,
                statusService,
                reloadConfigurationUseCase,
                migrateLegacyConfigurationUseCase,
                new DisabledNetworkBroadcastService(),
                new DisabledDiscordBridgeService());
    }

    public AnnouncerCommandService(
            AnnouncementRepository repository,
            AnnouncementDispatcher dispatcher,
            PlatformStatusService statusService,
            ReloadConfigurationUseCase reloadConfigurationUseCase,
            MigrateLegacyConfigurationUseCase migrateLegacyConfigurationUseCase,
            NetworkBroadcastService networkBroadcastService,
            DiscordBridgeService discordBridgeService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.reloadConfigurationUseCase = Objects.requireNonNull(reloadConfigurationUseCase, "reloadConfigurationUseCase");
        this.migrateLegacyConfigurationUseCase = Objects.requireNonNull(
                migrateLegacyConfigurationUseCase,
                "migrateLegacyConfigurationUseCase");
        this.networkBroadcastService = Objects.requireNonNull(networkBroadcastService, "networkBroadcastService");
        this.discordBridgeService = Objects.requireNonNull(discordBridgeService, "discordBridgeService");
    }

    public CommandOutcome handle(CommandRequest request) {
        if (request == null || request.arguments().isEmpty()) {
            return help();
        }
        String subcommand = request.argument(0).toLowerCase(Locale.ROOT);
        try {
            return switch (subcommand) {
                case "version" -> CommandOutcome.success("AdvancedAnnouncer running on "
                        + statusService.platformName() + " " + statusService.platformVersion()
                        + " (plugin " + statusService.pluginVersion() + ", Redis="
                        + enabled(statusService.redisEnabled()) + ", Discord="
                        + enabled(statusService.discordEnabled()) + ").");
                case "debug" -> debug();
                case "list" -> list();
                case "create" -> create(request);
                case "delete" -> delete(request);
                case "toggle" -> toggle(request);
                case "send" -> send(request);
                case "preview" -> preview(request);
                case "reload" -> reload();
                case "migrate" -> migrate();
                case "redis" -> redisDiagnostic(request);
                case "discord" -> discordDiagnostic(request);
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
                "PluginVersion=" + statusService.pluginVersion(),
                "Platform=" + statusService.platformName() + " " + statusService.platformVersion(),
                "Scheduler=" + enabled(statusService.schedulerEnabled()),
                "Folia=" + (statusService.foliaDetected() ? "detected" : "not detected"),
                "PlaceholderAPI=" + (statusService.placeholderApiAvailable() ? "available" : "absent"),
                "Redis=" + enabled(statusService.redisEnabled()),
                "Discord=" + enabled(statusService.discordEnabled()),
                "Announcements=" + repository.findAll().size()));
    }

    private String enabled(boolean value) {
        return value ? "enabled" : "disabled";
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

    private CommandOutcome redisDiagnostic(CommandRequest request) {
        if (request.arguments().size() >= 2 && "test".equalsIgnoreCase(request.argument(1))) {
            return CommandOutcome.success(networkBroadcastService.connected()
                    ? "Redis connection is available."
                    : "Redis is disabled or not connected.");
        }
        return CommandOutcome.error("Usage: /announcer redis test");
    }

    private CommandOutcome discordDiagnostic(CommandRequest request) {
        if (request.arguments().size() >= 2 && "test".equalsIgnoreCase(request.argument(1))) {
            if (!discordBridgeService.enabled()) {
                return CommandOutcome.success("Discord is disabled or not configured.");
            }
            discordBridgeService.send(new DiscordOutboundMessage(
                    "AdvancedAnnouncer Test",
                    "Discord integration test request sent from AdvancedAnnouncer."));
            return CommandOutcome.success("Discord test request sent.");
        }
        return CommandOutcome.error("Usage: /announcer discord test");
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

    private static final class DisabledNetworkBroadcastService implements NetworkBroadcastService {
        @Override
        public CompletableFuture<Void> publish(dev.studio.announcer.api.network.NetworkBroadcastRequest request) {
            return CompletableFuture.failedFuture(new IllegalStateException("Network broadcast service is disabled."));
        }

        @Override
        public void setHandler(Consumer<dev.studio.announcer.api.network.NetworkBroadcastRequest> handler) {
        }

        @Override
        public boolean connected() {
            return false;
        }

        @Override
        public void close() {
        }
    }

    private static final class DisabledDiscordBridgeService implements DiscordBridgeService {
        @Override
        public CompletableFuture<Void> send(dev.studio.announcer.api.discord.DiscordOutboundMessage message) {
            return CompletableFuture.failedFuture(new IllegalStateException("Discord bridge is disabled."));
        }

        @Override
        public void setInboundHandler(Consumer<dev.studio.announcer.api.discord.DiscordInboundMessage> handler) {
        }

        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public void close() {
        }
    }
}
