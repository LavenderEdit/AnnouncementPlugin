package dev.studio.announcer.spigot.bootstrap;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.api.service.SoundService;
import dev.studio.announcer.application.command.AnnouncerCommandService;
import dev.studio.announcer.application.usecase.CreateAnnouncementUseCase;
import dev.studio.announcer.application.usecase.PreviewAnnouncementUseCase;
import dev.studio.announcer.application.usecase.SendAnnouncementUseCase;
import dev.studio.announcer.application.usecase.ValidateAnnouncementUseCase;
import dev.studio.announcer.common.message.InternalPlaceholderResolver;
import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.common.repository.InMemoryAnnouncementRepository;
import dev.studio.announcer.spigot.adapter.SpigotAnnouncementDispatcher;
import dev.studio.announcer.spigot.adapter.SpigotAudienceProvider;
import dev.studio.announcer.spigot.command.AnnouncerCommand;
import dev.studio.announcer.spigot.placeholder.PlaceholderApiBridge;
import dev.studio.announcer.spigot.placeholder.SpigotPlaceholderResolver;
import dev.studio.announcer.spigot.scheduler.BukkitFoliaScheduler;
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
        announcementDispatcher = new SpigotAnnouncementDispatcher(
                audiences,
                renderer,
                new SpigotAudienceProvider(),
                scheduler,
                soundService);
        createAnnouncementUseCase = new CreateAnnouncementUseCase(announcementRepository);
        sendAnnouncementUseCase = new SendAnnouncementUseCase(announcementRepository, announcementDispatcher);
        previewAnnouncementUseCase = new PreviewAnnouncementUseCase(announcementRepository, announcementDispatcher);
        validateAnnouncementUseCase = new ValidateAnnouncementUseCase();
        announcerCommandService = new AnnouncerCommandService(
                announcementRepository,
                announcementDispatcher,
                platformStatusService);

        registerCommands();
        getLogger().info("AdvancedAnnouncer Phase 1 bootstrap enabled.");
    }

    @Override
    public void onDisable() {
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
}
