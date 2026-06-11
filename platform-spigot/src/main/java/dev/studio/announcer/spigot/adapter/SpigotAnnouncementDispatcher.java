package dev.studio.announcer.spigot.adapter;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.SoundService;
import dev.studio.announcer.api.service.ToastNotificationService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.option.ActionBarOptions;
import dev.studio.announcer.domain.announcement.option.BossBarOptions;
import dev.studio.announcer.domain.announcement.option.TitleOptions;
import dev.studio.announcer.domain.announcement.option.ToastOptions;
import dev.studio.announcer.spigot.service.PriorityActionBarService;
import dev.studio.announcer.spigot.service.PriorityBossBarService;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SpigotAnnouncementDispatcher implements AnnouncementDispatcher {
    private final BukkitAudiences audiences;
    private final MessageRenderer<Component> renderer;
    private final SpigotAudienceProvider audienceProvider;
    private final SoundService soundService;
    private final PriorityActionBarService actionBarService;
    private final PriorityBossBarService bossBarService;
    private final ToastNotificationService toastNotificationService;
    private final Set<String> serverGroups;

    public SpigotAnnouncementDispatcher(
            BukkitAudiences audiences,
            MessageRenderer<Component> renderer,
            SpigotAudienceProvider audienceProvider,
            SoundService soundService,
            PriorityActionBarService actionBarService,
            PriorityBossBarService bossBarService,
            ToastNotificationService toastNotificationService) {
        this(
                audiences,
                renderer,
                audienceProvider,
                soundService,
                actionBarService,
                bossBarService,
                toastNotificationService,
                Set.of());
    }

    public SpigotAnnouncementDispatcher(
            BukkitAudiences audiences,
            MessageRenderer<Component> renderer,
            SpigotAudienceProvider audienceProvider,
            SoundService soundService,
            PriorityActionBarService actionBarService,
            PriorityBossBarService bossBarService,
            ToastNotificationService toastNotificationService,
            Set<String> serverGroups) {
        this.audiences = audiences;
        this.renderer = renderer;
        this.audienceProvider = audienceProvider;
        this.soundService = soundService;
        this.actionBarService = actionBarService;
        this.bossBarService = bossBarService;
        this.toastNotificationService = toastNotificationService;
        this.serverGroups = serverGroups == null ? Set.of() : serverGroups;
    }

    @Override
    public DeliverySummary broadcast(Announcement announcement) {
        if (!announcement.enabled()) {
            return DeliverySummary.empty();
        }
        int delivered = 0;
        int skipped = 0;
        int invalid = 0;
        for (Player player : audienceProvider.onlinePlayers()) {
            if (canReceive(player, announcement)) {
                if (send(player, announcement)) {
                    delivered++;
                } else {
                    invalid++;
                }
            } else {
                skipped++;
            }
        }
        return new DeliverySummary(delivered, skipped, invalid);
    }

    @Override
    public DeliverySummary preview(Announcement announcement, String audienceId) {
        return audienceProvider.findPlayer(audienceId)
                .map(player -> send(player, announcement) ? new DeliverySummary(1, 0, 0) : new DeliverySummary(0, 0, 1))
                .orElseGet(() -> new DeliverySummary(0, 0, 1));
    }

    private boolean send(Player player, Announcement announcement) {
        PlaceholderContext context = contextFor(player, announcement);
        boolean sent = false;
        if (announcement.channels().contains(AnnouncementChannel.CHAT)) {
            for (String message : announcement.messages()) {
                audiences.player(player).sendMessage(renderer.render(message, context));
                sent = true;
            }
        }
        if (announcement.channels().contains(AnnouncementChannel.TITLE)
                || announcement.channels().contains(AnnouncementChannel.SUBTITLE)) {
            sendTitle(player, announcement, context);
            sent = true;
        }
        if (announcement.channels().contains(AnnouncementChannel.ACTIONBAR)) {
            sendActionBar(player, announcement, context);
            sent = true;
        }
        if (announcement.channels().contains(AnnouncementChannel.BOSSBAR)) {
            sent = sendBossBar(player, announcement, context) || sent;
        }
        if (announcement.channels().contains(AnnouncementChannel.TOAST)) {
            sent = sendToast(player, announcement, context) || sent;
        }
        if (announcement.channels().contains(AnnouncementChannel.SOUND)) {
            sent = announcement.soundOptions()
                    .map(options -> soundService.play(player.getUniqueId().toString(), options))
                    .orElse(sent);
        }
        return sent;
    }

    private boolean canReceive(Player player, Announcement announcement) {
        return announcement.permission()
                .map(player::hasPermission)
                .orElse(true);
    }

    private void sendTitle(Player player, Announcement announcement, PlaceholderContext context) {
        TitleOptions options = announcement.titleOptions().orElseGet(() -> new TitleOptions(
                firstMessage(announcement),
                "",
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofMillis(500)));
        Component title = renderer.render(options.title(), context);
        Component subtitle = renderer.render(options.subtitle(), context);
        audiences.player(player).showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(options.fadeIn(), options.stay(), options.fadeOut())));
    }

    private void sendActionBar(Player player, Announcement announcement, PlaceholderContext context) {
        ActionBarOptions options = announcement.actionBarOptions().orElseGet(() -> new ActionBarOptions(
                firstMessage(announcement),
                Duration.ofSeconds(2),
                announcement.priority(),
                null,
                Duration.ZERO));
        if (options.permissionValue().map(player::hasPermission).orElse(true)) {
            actionBarService.show(
                    player.getUniqueId().toString(),
                    announcement.id().value(),
                    renderer.render(options.message(), context),
                    options.priority(),
                    options.duration(),
                    options.antiSpamWindow());
        }
    }

    private boolean sendBossBar(Player player, Announcement announcement, PlaceholderContext context) {
        BossBarOptions options = announcement.bossBarOptions().orElseGet(() -> new BossBarOptions(
                fallbackVisualText(announcement),
                dev.studio.announcer.domain.announcement.option.BossBarColor.WHITE,
                dev.studio.announcer.domain.announcement.option.BossBarOverlay.PROGRESS,
                1.0f,
                Duration.ofSeconds(3),
                announcement.priority(),
                null,
                false,
                true));
        if (options.permissionValue().map(player::hasPermission).orElse(true)) {
            BossBar bossBar = BossBar.bossBar(
                    renderer.render(options.title(), context),
                    options.progress(),
                    BossBar.Color.valueOf(options.color().name()),
                    BossBar.Overlay.valueOf(options.overlay().name()));
            bossBarService.show(
                    player.getUniqueId().toString(),
                    announcement.id().value(),
                    bossBar,
                    options.priority(),
                    options.duration(),
                    options.animatedProgress(),
                    options.autoHide());
            return true;
        }
        return false;
    }

    private boolean sendToast(Player player, Announcement announcement, PlaceholderContext context) {
        ToastOptions options = announcement.toastOptions().orElse(null);
        if (options == null || !options.enabled()) {
            return false;
        }
        if (options.permissionValue().map(player::hasPermission).orElse(true)) {
            toastNotificationService.showToast(player.getUniqueId().toString(), options, context);
            return true;
        }
        return false;
    }

    private String firstMessage(Announcement announcement) {
        return announcement.messages().isEmpty() ? "" : announcement.messages().getFirst();
    }

    private String fallbackVisualText(Announcement announcement) {
        String message = firstMessage(announcement);
        return message.isBlank() ? announcement.name() : message;
    }

    private PlaceholderContext contextFor(Player player, Announcement announcement) {
        return PlaceholderContext.of(Map.of(
                "player_name", player.getName(),
                "player_displayname", player.getDisplayName(),
                "player_uuid", player.getUniqueId().toString(),
                "server_name", Bukkit.getServer().getName().toLowerCase(Locale.ROOT),
                "server_group", serverGroups.stream().sorted().collect(Collectors.joining(",")),
                "online_players", Integer.toString(Bukkit.getOnlinePlayers().size()),
                "max_players", Integer.toString(Bukkit.getMaxPlayers()),
                "world", player.getWorld().getName(),
                "announcement_id", announcement.id().value(),
                "announcement_name", announcement.name()));
    }
}
