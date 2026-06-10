package dev.studio.announcer.spigot.bootstrap;

import dev.studio.announcer.api.service.AvatarService;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.api.service.SoundService;
import dev.studio.announcer.api.service.ToastNotificationService;
import dev.studio.announcer.application.command.AnnouncerCommandService;
import dev.studio.announcer.application.usecase.CreateAnnouncementUseCase;
import dev.studio.announcer.application.usecase.PreviewAnnouncementUseCase;
import dev.studio.announcer.application.usecase.SendAnnouncementUseCase;
import dev.studio.announcer.application.usecase.ValidateAnnouncementUseCase;
import dev.studio.announcer.common.avatar.CachedAvatarService;
import dev.studio.announcer.common.avatar.InMemoryAvatarCacheStore;
import dev.studio.announcer.common.message.InternalPlaceholderResolver;
import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.common.repository.InMemoryAnnouncementRepository;
import dev.studio.announcer.spigot.adapter.SpigotAnnouncementDispatcher;
import dev.studio.announcer.spigot.adapter.SpigotAudienceProvider;
import dev.studio.announcer.spigot.avatar.SpigotProfileAvatarTextureSource;
import dev.studio.announcer.spigot.command.AnnouncerCommand;
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

public final class AdvancedAnnouncerPlugin extends JavaPlugin {
    private BukkitAudiences audiences;
    private AnnouncementRepository announcementRepository;
    private AnnouncementDispatcher announcementDispatcher;
    private SchedulerPort scheduler;
    private SoundService soundService;
    private ToastNotificationService toastNotificationService;
    private AvatarService avatarService;
    private PriorityActionBarService actionBarService;
    private PriorityBossBarService bossBarService;
    private SpigotPlatformStatusService platformStatusService;
    private CreateAnnouncementUseCase createAnnouncementUseCase;
    private SendAnnouncementUseCase sendAnnouncementUseCase;
    private PreviewAnnouncementUseCase previewAnnouncementUseCase;
    private ValidateAnnouncementUseCase validateAnnouncementUseCase;
    private AnnouncerCommandService announcerCommandService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        audiences = BukkitAudiences.create(this);
        announcementRepository = new InMemoryAnnouncementRepository();
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
        createAnnouncementUseCase = new CreateAnnouncementUseCase(announcementRepository);
        sendAnnouncementUseCase = new SendAnnouncementUseCase(announcementRepository, announcementDispatcher);
        previewAnnouncementUseCase = new PreviewAnnouncementUseCase(announcementRepository, announcementDispatcher);
        validateAnnouncementUseCase = new ValidateAnnouncementUseCase();
        announcerCommandService = new AnnouncerCommandService(
                announcementRepository,
                announcementDispatcher,
                platformStatusService);

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new NotificationCleanupListener(actionBarService, bossBarService),
                this);
        getLogger().info("AdvancedAnnouncer Phase 3 bootstrap enabled.");
    }

    @Override
    public void onDisable() {
        if (toastNotificationService instanceof AutoCloseable closeable) {
            closeQuietly(closeable);
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
        AnnouncerCommand executor = new AnnouncerCommand(announcerCommandService);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ex) {
            getLogger().warning("Failed to close service cleanly: " + ex.getMessage());
        }
    }
}
