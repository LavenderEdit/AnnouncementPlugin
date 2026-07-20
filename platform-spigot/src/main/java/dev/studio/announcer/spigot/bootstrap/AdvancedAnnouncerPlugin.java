package dev.studio.announcer.spigot.bootstrap;

import dev.studio.announcer.api.service.AvatarService;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementEditorService;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import dev.studio.announcer.api.service.DiscordBridgeService;
import dev.studio.announcer.api.service.NetworkBroadcastService;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.api.service.SoundService;
import dev.studio.announcer.api.service.ToastNotificationService;
import dev.studio.announcer.application.command.AnnouncerCommandService;
import dev.studio.announcer.application.usecase.CreateAnnouncementUseCase;
import dev.studio.announcer.application.usecase.HandleDiscordInboundUseCase;
import dev.studio.announcer.application.usecase.HandleNetworkBroadcastUseCase;
import dev.studio.announcer.application.usecase.MigrateLegacyConfigurationUseCase;
import dev.studio.announcer.application.usecase.PreviewAnnouncementUseCase;
import dev.studio.announcer.application.usecase.ReloadConfigurationUseCase;
import dev.studio.announcer.application.usecase.SendAnnouncementUseCase;
import dev.studio.announcer.application.usecase.ValidateAnnouncementUseCase;
import dev.studio.announcer.application.usecase.ExecuteTriggeredAnnouncementsUseCase;
import dev.studio.announcer.application.usecase.TriggerMatcher;
import dev.studio.announcer.common.avatar.CachedAvatarService;
import dev.studio.announcer.common.avatar.InMemoryAvatarCacheStore;
import dev.studio.announcer.common.config.ConfigBackupService;
import dev.studio.announcer.common.config.ConfigurationBootstrapper;
import dev.studio.announcer.common.config.ConfigurationPaths;
import dev.studio.announcer.common.config.ConfigurationValidationService;
import dev.studio.announcer.common.config.LegacyConfigurationMigrationService;
import dev.studio.announcer.common.config.LegacyWelcomeDonationsMigrator;
import dev.studio.announcer.common.config.YamlConfigurationReloadService;
import dev.studio.announcer.common.config.YamlAnnouncementRepository;
import dev.studio.announcer.common.discord.CompositeDiscordBridgeService;
import dev.studio.announcer.common.discord.DiscordWebhookBridgeService;
import dev.studio.announcer.common.discord.DiscordWebhookSettings;
import dev.studio.announcer.common.message.InternalPlaceholderResolver;
import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.common.network.JacksonNetworkBroadcastCodec;
import dev.studio.announcer.common.network.LettuceRedisPubSubClient;
import dev.studio.announcer.common.network.NetworkTargetFilter;
import dev.studio.announcer.common.network.RedisNetworkBroadcastService;
import dev.studio.announcer.common.scheduler.DefaultAnnouncementSchedulerService;
import dev.studio.announcer.common.service.NoopDiscordBridgeService;
import dev.studio.announcer.common.service.NoopNetworkBroadcastService;
import dev.studio.announcer.domain.validation.ValidationResult;
import dev.studio.announcer.spigot.adapter.SpigotAnnouncementDispatcher;
import dev.studio.announcer.spigot.adapter.SpigotAudienceProvider;
import dev.studio.announcer.spigot.avatar.SpigotProfileAvatarTextureSource;
import dev.studio.announcer.spigot.command.AnnouncerCommand;
import dev.studio.announcer.spigot.discord.DiscordSrvBridgeService;
import dev.studio.announcer.spigot.discord.DiscordSrvEventForwarder;
import dev.studio.announcer.spigot.discord.DiscordSrvSettings;
import dev.studio.announcer.spigot.gui.AnvilTextInputService;
import dev.studio.announcer.spigot.gui.SpigotAnnouncementEditorService;
import dev.studio.announcer.spigot.listener.NotificationCleanupListener;
import dev.studio.announcer.spigot.listener.PlayerJoinAnnouncementListener;
import dev.studio.announcer.spigot.placeholder.PlaceholderApiBridge;
import dev.studio.announcer.spigot.placeholder.SpigotPlaceholderResolver;
import dev.studio.announcer.spigot.scheduler.BukkitFoliaScheduler;
import dev.studio.announcer.spigot.service.AdventureActionBarSender;
import dev.studio.announcer.spigot.service.AdventureBossBarSender;
import dev.studio.announcer.spigot.service.BukkitToastSender;
import dev.studio.announcer.spigot.service.PriorityActionBarService;
import dev.studio.announcer.spigot.service.PriorityBossBarService;
import dev.studio.announcer.spigot.service.ScheduledToastNotificationService;
import dev.studio.announcer.spigot.service.SpigotPlatformStatusService;
import dev.studio.announcer.spigot.service.SpigotSoundService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdvancedAnnouncerPlugin extends JavaPlugin {
    private BukkitAudiences audiences;
    private AnnouncementRepository announcementRepository;
    private AnnouncementDispatcher announcementDispatcher;
    private SchedulerPort scheduler;
    private SoundService soundService;
    private NetworkBroadcastService networkBroadcastService;
    private DiscordBridgeService discordBridgeService;
    private AutoCloseable discordSrvForwarder;
    private ToastNotificationService toastNotificationService;
    private AvatarService avatarService;
    private AnnouncementEditorService announcementEditorService;
    private AnnouncementSchedulerService announcementSchedulerService;
    private PriorityActionBarService actionBarService;
    private PriorityBossBarService bossBarService;
    private ConfigurationValidationService configurationValidationService;
    private SpigotPlatformStatusService platformStatusService;
    private CreateAnnouncementUseCase createAnnouncementUseCase;
    private SendAnnouncementUseCase sendAnnouncementUseCase;
    private PreviewAnnouncementUseCase previewAnnouncementUseCase;
    private ValidateAnnouncementUseCase validateAnnouncementUseCase;
    private ReloadConfigurationUseCase reloadConfigurationUseCase;
    private MigrateLegacyConfigurationUseCase migrateLegacyConfigurationUseCase;
    private AnnouncerCommandService announcerCommandService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigurationPaths configurationPaths = ConfigurationPaths.fromDataDirectory(getDataFolder().toPath());
        new ConfigurationBootstrapper(configurationPaths).bootstrap();

        audiences = BukkitAudiences.create(this);
        YamlAnnouncementRepository yamlAnnouncementRepository = new YamlAnnouncementRepository(configurationPaths.announcementsDirectory());
        announcementRepository = yamlAnnouncementRepository;
        scheduler = new BukkitFoliaScheduler(this);
        soundService = new SpigotSoundService();
        SpigotPlaceholderResolver placeholderResolver = new SpigotPlaceholderResolver(
                new InternalPlaceholderResolver(),
                PlaceholderApiBridge.detect());
        MiniMessageComponentRenderer renderer = new MiniMessageComponentRenderer(placeholderResolver);
        configurationValidationService = new ConfigurationValidationService(renderer);
        actionBarService = new PriorityActionBarService(new AdventureActionBarSender(audiences), scheduler);
        bossBarService = new PriorityBossBarService(new AdventureBossBarSender(audiences), scheduler);
        toastNotificationService = new ScheduledToastNotificationService(new BukkitToastSender(this), renderer, scheduler);
        avatarService = new CachedAvatarService(
                new InMemoryAvatarCacheStore(),
                new SpigotProfileAvatarTextureSource());
        String serverId = getConfig().getString("server.id", getServer().getName().toLowerCase(java.util.Locale.ROOT));
        Set<String> serverGroups = stringSet(getConfig().getStringList("server.groups"));
        SpigotAudienceProvider audienceProvider = new SpigotAudienceProvider();
        announcementDispatcher = new SpigotAnnouncementDispatcher(
                audiences,
                renderer,
                audienceProvider,
                soundService,
                actionBarService,
                bossBarService,
                toastNotificationService,
                serverGroups);
        networkBroadcastService = createNetworkBroadcastService();
        HandleNetworkBroadcastUseCase handleNetworkBroadcastUseCase = new HandleNetworkBroadcastUseCase(
                announcementDispatcher,
                new NetworkTargetFilter(
                        serverId,
                        serverGroups,
                        getConfig().getBoolean("redis.ignore-self", true))::accepts);
        networkBroadcastService.setHandler(request -> scheduler.scheduleOnce(
                "network-inbound-" + request.messageId(),
                Duration.ZERO,
                () -> handleNetworkBroadcastUseCase.handle(request)));
        discordBridgeService = createDiscordBridgeService();
        HandleDiscordInboundUseCase handleDiscordInboundUseCase = new HandleDiscordInboundUseCase(
                announcementDispatcher,
                getConfig().getString(
                        "discord.discordsrv.minecraft-format",
                        "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>"));
        discordBridgeService.setInboundHandler(message -> scheduler.scheduleOnce(
                "discord-inbound-" + message.userId() + "-" + message.createdAt().toEpochMilli(),
                Duration.ZERO,
                () -> handleDiscordInboundUseCase.handle(message)));
        platformStatusService = new SpigotPlatformStatusService(
                getDescription().getVersion(),
                () -> announcementSchedulerService != null,
                () -> networkBroadcastService != null && networkBroadcastService.connected(),
                () -> discordBridgeService != null && discordBridgeService.enabled());
        announcementSchedulerService = new DefaultAnnouncementSchedulerService(scheduler, announcementDispatcher);
        announcementSchedulerService.reschedule(announcementRepository.findAll());
        createAnnouncementUseCase = new CreateAnnouncementUseCase(announcementRepository);
        sendAnnouncementUseCase = new SendAnnouncementUseCase(announcementRepository, announcementDispatcher);
        previewAnnouncementUseCase = new PreviewAnnouncementUseCase(announcementRepository, announcementDispatcher);
        validateAnnouncementUseCase = new ValidateAnnouncementUseCase();
        YamlConfigurationReloadService reloadService = new YamlConfigurationReloadService(
                configurationPaths,
                yamlAnnouncementRepository,
                configurationValidationService,
                announcementSchedulerService,
                this::reloadConfig);
        reloadConfigurationUseCase = new ReloadConfigurationUseCase(reloadService);
        migrateLegacyConfigurationUseCase = new MigrateLegacyConfigurationUseCase(new LegacyConfigurationMigrationService(
                legacyConfigPath(configurationPaths),
                configurationPaths,
                announcementRepository,
                new ConfigBackupService(configurationPaths.backupsDirectory()),
                reloadService,
                new LegacyWelcomeDonationsMigrator()));
        AnvilTextInputService anvilInputService = new AnvilTextInputService(this, renderer, audienceProvider);
        announcementEditorService = new SpigotAnnouncementEditorService(
                this,
                announcementRepository,
                announcementDispatcher,
                scheduler,
                reloadConfigurationUseCase,
                anvilInputService);
        announcerCommandService = new AnnouncerCommandService(
                announcementRepository,
                announcementDispatcher,
                platformStatusService,
                reloadConfigurationUseCase,
                migrateLegacyConfigurationUseCase,
                networkBroadcastService,
                discordBridgeService);

        Duration defaultJoinDelay = Duration.ZERO;
        String rawJoinDelay = getConfig().getString("events.join.delay", "PT0.5S");
        try {
            defaultJoinDelay = Duration.parse(rawJoinDelay);
        } catch (Exception e) {
            getLogger().warning("Invalid default join delay format '" + rawJoinDelay + "', defaulting to 0s.");
        }
        ExecuteTriggeredAnnouncementsUseCase executeTriggeredAnnouncementsUseCase = new ExecuteTriggeredAnnouncementsUseCase(
                announcementRepository,
                announcementDispatcher,
                scheduler,
                new TriggerMatcher(serverId, serverGroups),
                defaultJoinDelay);

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new NotificationCleanupListener(actionBarService, bossBarService),
                this);
        getServer().getPluginManager().registerEvents(anvilInputService, this);
        getServer().getPluginManager().registerEvents(
                new PlayerJoinAnnouncementListener(executeTriggeredAnnouncementsUseCase),
                this);
        if (announcementEditorService instanceof SpigotAnnouncementEditorService spigotEditorService) {
            getServer().getPluginManager().registerEvents(spigotEditorService, this);
        }

        int announcementCount = announcementRepository.findAll().size();
        boolean redisEnabled = getConfig().getBoolean("redis.enabled", false);
        boolean discordEnabled = getConfig().getBoolean("discord.enabled", false);
        boolean foliaReady = scheduler instanceof dev.studio.announcer.spigot.scheduler.BukkitFoliaScheduler;
        StartupBanner.printEnable(getLogger(), getDescription().getVersion(),
                announcementCount, redisEnabled, discordEnabled, foliaReady);
    }

    @Override
    public void onDisable() {
        if (toastNotificationService instanceof AutoCloseable closeable) {
            closeQuietly(closeable);
        }
        if (discordSrvForwarder != null) {
            closeQuietly(discordSrvForwarder);
            discordSrvForwarder = null;
        }
        if (discordBridgeService != null) {
            closeQuietly(discordBridgeService);
            discordBridgeService = null;
        }
        if (networkBroadcastService != null) {
            closeQuietly(networkBroadcastService);
            networkBroadcastService = null;
        }
        if (announcementSchedulerService != null) {
            closeQuietly(announcementSchedulerService);
            announcementSchedulerService = null;
        }
        if (announcementEditorService instanceof AutoCloseable closeable) {
            closeQuietly(closeable);
            announcementEditorService = null;
        }
        if (bossBarService != null) {
            closeQuietly(bossBarService);
            bossBarService = null;
        }
        if (actionBarService != null) {
            closeQuietly(actionBarService);
            actionBarService = null;
        }
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
        StartupBanner.printDisable(getLogger(), getDescription().getVersion());
    }

    private void registerCommands() {
        PluginCommand command = getCommand("announcer");
        if (command == null) {
            getLogger().warning("Command 'announcer' is missing from plugin.yml.");
            return;
        }
        AnnouncerCommand executor = new AnnouncerCommand(announcerCommandService, announcementEditorService);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private Path legacyConfigPath(ConfigurationPaths configurationPaths) {
        Path dataFolderLegacy = configurationPaths.dataDirectory().resolve("legacy").resolve("config.yml");
        if (Files.exists(dataFolderLegacy)) {
            return dataFolderLegacy;
        }
        return Path.of("legacy", "welcomedonations-maven", "src", "main", "resources", "config.yml");
    }

    private NetworkBroadcastService createNetworkBroadcastService() {
        if (!getConfig().getBoolean("redis.enabled", false)) {
            return new NoopNetworkBroadcastService();
        }
        ValidationResult validation = configurationValidationService.validateRedisSettings(
                true,
                getConfig().getString("redis.uri", "redis://localhost:6379"),
                getConfig().getString("redis.channel", "advanced_announcer:broadcast"),
                getConfig().getLong("redis.reconnect-delay-seconds", 5L));
        if (!validation.valid()) {
            validation.errors().forEach(error -> getLogger().warning("Redis integration disabled: " + error));
            return new NoopNetworkBroadcastService();
        }
        try {
            return new RedisNetworkBroadcastService(
                    new LettuceRedisPubSubClient(getConfig().getString("redis.uri", "redis://localhost:6379")),
                    new JacksonNetworkBroadcastCodec(),
                    getConfig().getString("redis.channel", "advanced_announcer:broadcast"));
        } catch (RuntimeException ex) {
            getLogger().warning("Redis integration disabled after connection failure: " + ex.getMessage());
            return new NoopNetworkBroadcastService();
        }
    }

    private DiscordBridgeService createDiscordBridgeService() {
        DiscordBridgeService webhook = createDiscordWebhookBridgeService();
        DiscordSrvBridgeService discordSrv = createDiscordSrvBridgeService();
        if (discordSrv.enabled() && getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
            DiscordSrvEventForwarder forwarder = new DiscordSrvEventForwarder(discordSrv);
            forwarder.register();
            discordSrvForwarder = forwarder;
        }
        return new CompositeDiscordBridgeService(webhook, discordSrv.enabled() ? discordSrv : new NoopDiscordBridgeService());
    }

    private DiscordBridgeService createDiscordWebhookBridgeService() {
        if (!getConfig().getBoolean("discord.enabled", false)
                || !getConfig().getBoolean("discord.webhook.enabled", false)) {
            return new NoopDiscordBridgeService();
        }
        ValidationResult validation = configurationValidationService.validateDiscordSettings(
                true,
                true,
                getConfig().getString("discord.webhook.url", ""),
                getConfig().getString(
                        "discord.discordsrv.minecraft-format",
                        "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>"),
                getConfig().getLong("discord.discordsrv.cooldown-seconds", 3L));
        if (!validation.valid()) {
            validation.errors().forEach(error -> getLogger().warning("Discord webhook integration disabled: " + error));
            return new NoopDiscordBridgeService();
        }
        return new DiscordWebhookBridgeService(new DiscordWebhookSettings(
                getConfig().getString("discord.webhook.url", ""),
                getConfig().getString("discord.webhook.username", "AdvancedAnnouncer"),
                getConfig().getInt("discord.webhook.color", 0xF6C344),
                getConfig().getString("discord.webhook.footer", ""),
                getConfig().getString("discord.webhook.thumbnail-url", ""),
                getConfig().getBoolean("discord.webhook.timestamp", true)));
    }

    private DiscordSrvBridgeService createDiscordSrvBridgeService() {
        boolean enabled = getConfig().getBoolean("discord.enabled", false)
                && getConfig().getBoolean("discord.discordsrv.enabled", false)
                && getServer().getPluginManager().getPlugin("DiscordSRV") != null;
        ValidationResult validation = configurationValidationService.validateDiscordSettings(
                getConfig().getBoolean("discord.enabled", false),
                false,
                "",
                getConfig().getString(
                        "discord.discordsrv.minecraft-format",
                        "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>"),
                getConfig().getLong("discord.discordsrv.cooldown-seconds", 3L));
        if (!validation.valid()) {
            validation.errors().forEach(error -> getLogger().warning("DiscordSRV integration disabled: " + error));
            enabled = false;
        }
        return new DiscordSrvBridgeService(new DiscordSrvSettings(
                enabled,
                stringSet(getConfig().getStringList("discord.discordsrv.channel-whitelist")),
                stringSet(getConfig().getStringList("discord.discordsrv.allowed-role-ids")),
                Duration.ofSeconds(getConfig().getLong("discord.discordsrv.cooldown-seconds", 3L)),
                getConfig().getString(
                        "discord.discordsrv.minecraft-format",
                        "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>")));
    }

    private Set<String> stringSet(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ex) {
            getLogger().warning("Failed to close service cleanly: " + ex.getMessage());
        }
    }
}
