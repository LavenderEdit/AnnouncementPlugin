package dev.studio.announcer.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementManagementResult;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.DiscordBridgeService;
import dev.studio.announcer.api.service.NetworkBroadcastService;
import dev.studio.announcer.api.service.PlatformStatusService;
import dev.studio.announcer.application.usecase.MigrateLegacyConfigurationUseCase;
import dev.studio.announcer.application.usecase.ReloadConfigurationUseCase;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AnnouncerCommandServiceTest {

    @Test
    void createListToggleDeleteLifecycleUsesRepository() {
        FakeRepository repository = new FakeRepository();
        AnnouncerCommandService service = service(repository, new FakeDispatcher());

        assertEquals(CommandOutcome.success("Created announcement 'sale'."), service.handle(CommandRequest.console("create", "sale")));
        assertTrue(service.handle(CommandRequest.console("list")).messages().getFirst().contains("sale"));
        assertEquals(CommandOutcome.success("Announcement 'sale' disabled."), service.handle(CommandRequest.console("toggle", "sale")));
        assertTrue(!repository.findById(AnnouncementId.of("sale")).orElseThrow().enabled());
        assertEquals(CommandOutcome.success("Deleted announcement 'sale'."), service.handle(CommandRequest.console("delete", "sale")));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void sendAndPreviewRouteToDispatcher() {
        FakeRepository repository = new FakeRepository();
        FakeDispatcher dispatcher = new FakeDispatcher();
        repository.save(sample("alert"));
        AnnouncerCommandService service = service(repository, dispatcher);

        CommandOutcome send = service.handle(CommandRequest.console("send", "alert"));
        CommandOutcome preview = service.handle(CommandRequest.player("admin-uuid", "preview", "alert"));

        assertEquals(CommandStatus.SUCCESS, send.status());
        assertEquals(CommandStatus.SUCCESS, preview.status());
        assertEquals(List.of("alert"), dispatcher.broadcasted);
        assertEquals(List.of("admin-uuid:alert"), dispatcher.previews);
    }

    @Test
    void debugReportsPlatformStatusWithoutRedisOrDiscord() {
        AnnouncerCommandService service = service(new FakeRepository(), new FakeDispatcher());

        CommandOutcome outcome = service.handle(CommandRequest.console("debug"));

        assertEquals(CommandStatus.SUCCESS, outcome.status());
        assertTrue(outcome.messages().stream().anyMatch(line -> line.contains("Redis=disabled")));
        assertTrue(outcome.messages().stream().anyMatch(line -> line.contains("Discord=disabled")));
        assertTrue(outcome.messages().stream().anyMatch(line -> line.contains("Scheduler=enabled")));
        assertTrue(outcome.messages().stream().anyMatch(line -> line.contains("PluginVersion=0.1.0-test")));
    }

    @Test
    void redisAndDiscordTestReportDisabledByDefault() {
        AnnouncerCommandService service = service(new FakeRepository(), new FakeDispatcher());

        assertEquals(CommandOutcome.success("Redis is disabled or not connected."),
                service.handle(CommandRequest.console("redis", "test")));
        assertEquals(CommandOutcome.success("Discord is disabled or not configured."),
                service.handle(CommandRequest.console("discord", "test")));
    }

    @Test
    void redisAndDiscordTestReportConnectedServices() {
        AnnouncerCommandService service = new AnnouncerCommandService(
                new FakeRepository(),
                new FakeDispatcher(),
                new FakeStatus(),
                new NoOpManagementService(new FakeRepository()),
                new ReloadConfigurationUseCase(() -> ConfigurationReloadResult.success("Reloaded.")),
                new MigrateLegacyConfigurationUseCase(() -> ConfigurationReloadResult.success("Migrated.")),
                new FakeNetwork(true),
                new FakeDiscord(true));

        assertEquals(CommandOutcome.success("Redis connection is available."),
                service.handle(CommandRequest.console("redis", "test")));
        assertEquals(CommandOutcome.success("Discord test request sent."),
                service.handle(CommandRequest.console("discord", "test")));
    }

    @Test
    void reloadDelegatesToReloadUseCase() {
        AnnouncerCommandService service = new AnnouncerCommandService(
                new FakeRepository(),
                new FakeDispatcher(),
                new FakeStatus(),
                new NoOpManagementService(new FakeRepository()),
                new ReloadConfigurationUseCase(() -> ConfigurationReloadResult.success("Reloaded 3 announcement(s).")));

        assertEquals(CommandOutcome.success("Reloaded 3 announcement(s)."),
                service.handle(CommandRequest.console("reload")));
    }

    @Test
    void migrateDelegatesToMigrationUseCase() {
        AnnouncerCommandService service = new AnnouncerCommandService(
                new FakeRepository(),
                new FakeDispatcher(),
                new FakeStatus(),
                new NoOpManagementService(new FakeRepository()),
                new ReloadConfigurationUseCase(() -> ConfigurationReloadResult.success("Reloaded.")),
                new MigrateLegacyConfigurationUseCase(() -> ConfigurationReloadResult.success("Migrated 2 announcement(s).")));

        assertEquals(CommandOutcome.success("Migrated 2 announcement(s)."),
                service.handle(CommandRequest.console("migrate")));
    }

    private static AnnouncerCommandService service(FakeRepository repository, FakeDispatcher dispatcher) {
        return new AnnouncerCommandService(repository, dispatcher, new FakeStatus(), new NoOpManagementService(repository));
    }

    private static final class NoOpManagementService implements dev.studio.announcer.api.service.AnnouncementManagementService {
        private final FakeRepository repository;

        NoOpManagementService(FakeRepository repository) {
            this.repository = repository;
        }

        @Override public AnnouncementManagementResult create(dev.studio.announcer.domain.announcement.Announcement a) {
            repository.save(a);
            return AnnouncementManagementResult.success("Created announcement '" + a.id().value() + "'.");
        }
        @Override public AnnouncementManagementResult update(dev.studio.announcer.domain.announcement.Announcement a) {
            repository.save(a);
            return AnnouncementManagementResult.success("Updated announcement '" + a.id().value() + "'.");
        }
        @Override public AnnouncementManagementResult toggle(dev.studio.announcer.domain.announcement.AnnouncementId id) {
            dev.studio.announcer.domain.announcement.Announcement current = repository.findById(id).orElse(null);
            if (current == null) return AnnouncementManagementResult.failure("Announcement not found: " + id.value());
            repository.save(current.withEnabled(!current.enabled()));
            return AnnouncementManagementResult.success("Announcement '" + id.value() + "' " + (!current.enabled() ? "enabled" : "disabled") + ".");
        }
        @Override public AnnouncementManagementResult delete(dev.studio.announcer.domain.announcement.AnnouncementId id) {
            repository.deleteById(id);
            return AnnouncementManagementResult.success("Deleted announcement '" + id.value() + "'.");
        }
        @Override public AnnouncementManagementResult duplicate(dev.studio.announcer.domain.announcement.AnnouncementId s, dev.studio.announcer.domain.announcement.AnnouncementId t) {
            return AnnouncementManagementResult.success("Duplicated");
        }
        @Override public AnnouncementManagementResult saveAll() { return AnnouncementManagementResult.success("Saved"); }
        @Override public AnnouncementManagementResult validate(dev.studio.announcer.domain.announcement.Announcement a) { return AnnouncementManagementResult.success("Valid"); }
        @Override public java.util.Optional<dev.studio.announcer.domain.announcement.Announcement> findById(dev.studio.announcer.domain.announcement.AnnouncementId id) { return repository.findById(id); }
    }

    private static Announcement sample(String id) {
        return Announcement.builder(AnnouncementId.of(id), id)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("<green>" + id + "</green>"))
                .build();
    }

    private static final class FakeRepository implements AnnouncementRepository {
        private final Map<AnnouncementId, Announcement> announcements = new LinkedHashMap<>();

        @Override
        public Announcement save(Announcement announcement) {
            announcements.put(announcement.id(), announcement);
            return announcement;
        }

        @Override
        public Optional<Announcement> findById(AnnouncementId id) {
            return Optional.ofNullable(announcements.get(id));
        }

        @Override
        public List<Announcement> findAll() {
            return new ArrayList<>(announcements.values());
        }

        @Override
        public boolean deleteById(AnnouncementId id) {
            return announcements.remove(id) != null;
        }
    }

    private static final class FakeDispatcher implements AnnouncementDispatcher {
        private final List<String> broadcasted = new ArrayList<>();
        private final List<String> previews = new ArrayList<>();

        @Override
        public DeliverySummary broadcast(Announcement announcement) {
            broadcasted.add(announcement.id().value());
            return new DeliverySummary(1, 0, 0);
        }

        @Override
        public DeliverySummary preview(Announcement announcement, String audienceId) {
            previews.add(audienceId + ":" + announcement.id().value());
            return new DeliverySummary(1, 0, 0);
        }
    }

    private static final class FakeStatus implements PlatformStatusService {
        @Override
        public String platformName() {
            return "Paper";
        }

        @Override
        public String platformVersion() {
            return "1.21.1";
        }

        @Override
        public String pluginVersion() {
            return "0.1.0-test";
        }

        @Override
        public boolean schedulerEnabled() {
            return true;
        }

        @Override
        public boolean foliaDetected() {
            return false;
        }

        @Override
        public boolean placeholderApiAvailable() {
            return false;
        }

        @Override
        public boolean redisEnabled() {
            return false;
        }

        @Override
        public boolean discordEnabled() {
            return false;
        }
    }

    private static final class FakeNetwork implements NetworkBroadcastService {
        private final boolean connected;

        private FakeNetwork(boolean connected) {
            this.connected = connected;
        }

        @Override
        public CompletableFuture<Void> publish(dev.studio.announcer.api.network.NetworkBroadcastRequest request) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void setHandler(Consumer<dev.studio.announcer.api.network.NetworkBroadcastRequest> handler) {
        }

        @Override
        public boolean connected() {
            return connected;
        }

        @Override
        public void close() {
        }
    }

    private static final class FakeDiscord implements DiscordBridgeService {
        private final boolean enabled;
        private final List<dev.studio.announcer.api.discord.DiscordOutboundMessage> sent = new ArrayList<>();

        private FakeDiscord(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public CompletableFuture<Void> send(dev.studio.announcer.api.discord.DiscordOutboundMessage message) {
            sent.add(message);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void setInboundHandler(Consumer<dev.studio.announcer.api.discord.DiscordInboundMessage> handler) {
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public void close() {
        }
    }
}
