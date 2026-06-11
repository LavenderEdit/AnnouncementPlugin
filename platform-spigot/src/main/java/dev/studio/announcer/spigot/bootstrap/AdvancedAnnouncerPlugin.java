package dev.studio.announcer.spigot.bootstrap;

import dev.studio.announcer.api.service.AvatarService;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementEditorService;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.api.service.SoundService;
import dev.studio.announcer.api.service.ToastNotificationService;
import dev.studio.announcer.application.command.AnnouncerCommandService;
import dev.studio.announcer.application.usecase.CreateAnnouncementUseCase;
import dev.studio.announcer.application.usecase.MigrateLegacyConfigurationUseCase;
import dev.studio.announcer.application.usecase.PreviewAnnouncementUseCase;
import dev.studio.announcer.application.usecase.ReloadConfigurationUseCase;
import dev.studio.announcer.application.usecase.SendAnnouncementUseCase;
import dev.studio.announcer.application.usecase.ValidateAnnouncementUseCase;
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
import dev.studio.announcer.common.message.InternalPlaceholderResolver;
import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.common.scheduler.DefaultAnnouncementSchedulerService;
import dev.studio.announcer.spigot.adapter.SpigotAnnouncementDispatcher;
import dev.studio.announcer.spigot.adapter.SpigotAudienceProvider;
import dev.studio.announcer.spigot.avatar.SpigotProfileAvatarTextureSource;
import dev.studio.announcer.spigot.command.AnnouncerCommand;
import dev.studio.announcer.spigot.gui.AnvilTextInputService;
import dev.studio.announcer.spigot.gui.SpigotAnnouncementEditorService;
import dev.studio.announcer.spigot.listener.NotificationCleanupListener;
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

public final class AdvancedAnnouncerPlugin extends JavaPlugin {
    private BukkitAudiences audiences;
    private AnnouncementRepository announcementRepository;
    private AnnouncementDispatcher announcementDispatcher;
    private SchedulerPort scheduler;
    private SoundService soundService;
    private ToastNotificationService toastNotificationService;
    private AvatarService avatarService;
    private AnnouncementEditorService announcementEditorService;
    private AnnouncementSchedulerService announcementSchedulerService;
    private PriorityActionBarService actionBarService;
    private PriorityBossBarService bossBarService;
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
        platformStatusService = new SpigotPlatformStatusService();
        SpigotPlaceholderResolver placeholderResolver = new SpigotPlaceholderResolver(
                new InternalPlaceholderResolver(),
                PlaceholderApiBridge.detect());
        MiniMessageComponentRenderer renderer = new MiniMessageComponentRenderer(placeholderResolver);
        actionBarService = new PriorityActionBarService(new AdventureActionBarSender(audiences), scheduler);
        bossBarService = new PriorityBossBarService(new AdventureBossBarSender(audiences), scheduler);
        toastNotificationService = new ScheduledToastNotificationService(new BukkitToastSender(this), renderer, scheduler);
        avatarService = new CachedAvatarService(
                new InMemoryAvatarCacheStore(),
                new SpigotProfileAvatarTextureSource());
        announcementDispatcher = new SpigotAnnouncementDispatcher(
                audiences,
                renderer,
                new SpigotAudienceProvider(),
                soundService,
                actionBarService,
                bossBarService,
                toastNotificationService);
        announcementSchedulerService = new DefaultAnnouncementSchedulerService(scheduler, announcementDispatcher);
        announcementSchedulerService.reschedule(announcementRepository.findAll());
        createAnnouncementUseCase = new CreateAnnouncementUseCase(announcementRepository);
        sendAnnouncementUseCase = new SendAnnouncementUseCase(announcementRepository, announcementDispatcher);
        previewAnnouncementUseCase = new PreviewAnnouncementUseCase(announcementRepository, announcementDispatcher);
        validateAnnouncementUseCase = new ValidateAnnouncementUseCase();
        YamlConfigurationReloadService reloadService = new YamlConfigurationReloadService(
                configurationPaths,
                yamlAnnouncementRepository,
                new ConfigurationValidationService(renderer),
                announcementSchedulerService);
        reloadConfigurationUseCase = new ReloadConfigurationUseCase(reloadService);
        migrateLegacyConfigurationUseCase = new MigrateLegacyConfigurationUseCase(new LegacyConfigurationMigrationService(
                legacyConfigPath(configurationPaths),
                configurationPaths,
                announcementRepository,
                new ConfigBackupService(configurationPaths.backupsDirectory()),
                reloadService,
                new LegacyWelcomeDonationsMigrator()));
        announcementEditorService = new SpigotAnnouncementEditorService(
                this,
                announcementRepository,
                announcementDispatcher,
                scheduler,
                reloadConfigurationUseCase,
                new AnvilTextInputService(renderer));
        announcerCommandService = new AnnouncerCommandService(
                announcementRepository,
                announcementDispatcher,
                platformStatusService,
                reloadConfigurationUseCase,
                migrateLegacyConfigurationUseCase);

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new NotificationCleanupListener(actionBarService, bossBarService),
                this);
        if (announcementEditorService instanceof SpigotAnnouncementEditorService spigotEditorService) {
            getServer().getPluginManager().registerEvents(spigotEditorService, this);
        }
        getLogger().info("AdvancedAnnouncer Phase 4 bootstrap enabled.");
    }

    @Override
    public void onDisable() {
        if (toastNotificationService instanceof AutoCloseable closeable) {
            closeQuietly(closeable);
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
        getLogger().info("AdvancedAnnouncer disabled.");
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

    private void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ex) {
            getLogger().warning("Failed to close service cleanly: " + ex.getMessage());
        }
    }
}
