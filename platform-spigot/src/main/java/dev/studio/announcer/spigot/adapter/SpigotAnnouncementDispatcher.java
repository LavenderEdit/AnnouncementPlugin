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
import dev.studio.announcer.api.audience.Audience;
import dev.studio.announcer.api.audience.ActorAudience;
import dev.studio.announcer.api.audience.AllAudience;
import dev.studio.announcer.api.audience.OthersAudience;
import dev.studio.announcer.api.audience.PlayerAudience;
import dev.studio.announcer.api.placeholder.PlaceholderResolver;
import dev.studio.announcer.common.message.InternalPlaceholderResolver;
import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
        return dispatch(announcement, Audience.all(), null);
    }

    @Override
    public DeliverySummary preview(Announcement announcement, String audienceId) {
        return dispatch(announcement, Audience.player(audienceId), null);
    }

    @Override
    public DeliverySummary dispatch(Announcement announcement, Audience audience, String actorId) {
        if (!announcement.enabled()) {
            return DeliverySummary.empty();
        }

        Player actor = null;
        if (actorId != null && !actorId.isBlank()) {
            actor = audienceProvider.findPlayer(actorId).orElse(null);
        }

        int delivered = 0;
        int skipped = 0;
        int invalid = 0;

        if (audience instanceof ActorAudience) {
            if (actor == null) {
                return DeliverySummary.empty();
            }
            if (canReceive(actor, actor, announcement)) {
                if (send(actor, actor, announcement)) {
                    delivered++;
                } else {
                    invalid++;
                }
            } else {
                skipped++;
            }
        } else if (audience instanceof PlayerAudience playerAudience) {
            Player target = audienceProvider.findPlayer(playerAudience.playerId()).orElse(null);
            if (target == null) {
                return new DeliverySummary(0, 0, 1);
            }
            if (canReceive(target, actor, announcement)) {
                if (send(target, actor, announcement)) {
                    delivered++;
                } else {
                    invalid++;
                }
            } else {
                skipped++;
            }
        } else if (audience instanceof OthersAudience) {
            UUID actorUuid = actor != null ? actor.getUniqueId() : null;
            for (Player player : audienceProvider.onlinePlayers()) {
                if (actorUuid != null && player.getUniqueId().equals(actorUuid)) {
                    skipped++;
                    continue;
                }
                if (canReceive(player, actor, announcement)) {
                    if (send(player, actor, announcement)) {
                        delivered++;
                    } else {
                        invalid++;
                    }
                } else {
                    skipped++;
                }
            }
        } else {
            // Default to ALL (AllAudience)
            for (Player player : audienceProvider.onlinePlayers()) {
                if (canReceive(player, actor, announcement)) {
                    if (send(player, actor, announcement)) {
                        delivered++;
                    } else {
                        invalid++;
                    }
                } else {
                    skipped++;
                }
            }
        }

        return new DeliverySummary(delivered, skipped, invalid);
    }

    private boolean send(Player player, Announcement announcement) {
        return send(player, null, announcement);
    }

    private boolean send(Player player, Player actor, Announcement announcement) {
        PlaceholderContext context = contextFor(player, actor, announcement);
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
        return canReceive(player, null, announcement);
    }

    private boolean canReceive(Player player, Player actor, Announcement announcement) {
        if (!announcement.permission().map(player::hasPermission).orElse(true)) {
            return false;
        }
        if (!checkConditions(player, actor, announcement)) {
            return false;
        }
        return true;
    }

    private boolean checkConditions(Player player, Player actor, Announcement announcement) {
        if (announcement.conditions().isEmpty()) {
            return true;
        }
        PlaceholderResolver resolver = null;
        if (renderer instanceof MiniMessageComponentRenderer mmRenderer) {
            resolver = mmRenderer.placeholderResolver();
        } else {
            resolver = new InternalPlaceholderResolver();
        }
        PlaceholderContext context = contextFor(player, actor, announcement);
        for (String condition : announcement.conditions()) {
            if (!evaluateCondition(condition, context, resolver)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(String condition, PlaceholderContext context, PlaceholderResolver resolver) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        String cleanCondition = condition.trim();
        Boolean joinStateResult = evaluateJoinStateCondition(cleanCondition, context);
        if (joinStateResult != null) {
            return joinStateResult;
        }
        String resolved = resolver.resolve(condition, context).trim();
        String operator = null;
        int opIdx = -1;
        if (resolved.contains(">=")) {
            operator = ">=";
            opIdx = resolved.indexOf(">=");
        } else if (resolved.contains("<=")) {
            operator = "<=";
            opIdx = resolved.indexOf("<=");
        } else if (resolved.contains("==")) {
            operator = "==";
            opIdx = resolved.indexOf("==");
        } else if (resolved.contains("!=")) {
            operator = "!=";
            opIdx = resolved.indexOf("!=");
        } else if (resolved.contains(">")) {
            operator = ">";
            opIdx = resolved.indexOf(">");
        } else if (resolved.contains("<")) {
            operator = "<";
            opIdx = resolved.indexOf("<");
        } else if (resolved.contains("=")) {
            operator = "=";
            opIdx = resolved.indexOf("=");
        }
        if (operator == null) {
            return resolved.equalsIgnoreCase("true") || resolved.equalsIgnoreCase("yes") || resolved.equalsIgnoreCase("1");
        }
        String left = resolved.substring(0, opIdx).trim();
        String right = resolved.substring(opIdx + operator.length()).trim();
        try {
            double leftNum = Double.parseDouble(left);
            double rightNum = Double.parseDouble(right);
            switch (operator) {
                case ">=": return leftNum >= rightNum;
                case "<=": return leftNum <= rightNum;
                case ">": return leftNum > rightNum;
                case "<": return leftNum < rightNum;
                case "==":
                case "=": return leftNum == rightNum;
                case "!=": return leftNum != rightNum;
            }
        } catch (NumberFormatException e) {
            if (left.startsWith("\"") && left.endsWith("\"")) {
                left = left.substring(1, left.length() - 1);
            } else if (left.startsWith("'") && left.endsWith("'")) {
                left = left.substring(1, left.length() - 1);
            }
            if (right.startsWith("\"") && right.endsWith("\"")) {
                right = right.substring(1, right.length() - 1);
            } else if (right.startsWith("'") && right.endsWith("'")) {
                right = right.substring(1, right.length() - 1);
            }
            switch (operator) {
                case "==":
                case "=": return left.equalsIgnoreCase(right);
                case "!=": return !left.equalsIgnoreCase(right);
                default: return false;
            }
        }
        return false;
    }

    private Boolean evaluateJoinStateCondition(String cleanCondition, PlaceholderContext context) {
        if (cleanCondition.equalsIgnoreCase("join-state: FIRST_JOIN")) {
            return context.value("join_state").orElse("RECURRING").equals("FIRST_JOIN");
        }
        if (cleanCondition.equalsIgnoreCase("join-state: RECURRING")) {
            return context.value("join_state").orElse("RECURRING").equals("RECURRING");
        }
        return null;
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
        return contextFor(player, null, announcement);
    }

    private PlaceholderContext contextFor(Player recipient, Player actor, Announcement announcement) {
        Map<String, String> values = new HashMap<>();
        
        // Viewer placeholders
        values.put("viewer_name", recipient.getName());
        values.put("viewer_displayname", recipient.getDisplayName());
        values.put("viewer_uuid", recipient.getUniqueId().toString());
        values.put("viewer_world", recipient.getWorld().getName());
        
        // Backwards compatibility player placeholders
        values.put("player_name", recipient.getName());
        values.put("player_displayname", recipient.getDisplayName());
        values.put("player_uuid", recipient.getUniqueId().toString());
        values.put("player_world", recipient.getWorld().getName());
        values.put("world", recipient.getWorld().getName());
        
        if (actor != null) {
            values.put("actor_name", actor.getName());
            values.put("actor_displayname", actor.getDisplayName());
            values.put("actor_uuid", actor.getUniqueId().toString());
            values.put("actor_world", actor.getWorld().getName());
            boolean isFirst = !actor.hasPlayedBefore();
            values.put("join_state", isFirst ? "FIRST_JOIN" : "RECURRING");
        } else {
            values.put("actor_name", "");
            values.put("actor_displayname", "");
            values.put("actor_uuid", "");
            values.put("actor_world", "");
            values.put("join_state", "RECURRING");
        }
        
        values.put("server_name", audienceProvider.serverName().toLowerCase(Locale.ROOT));
        values.put("server_group", serverGroups.stream().sorted().collect(Collectors.joining(",")));
        values.put("online_players", Integer.toString(audienceProvider.onlinePlayersCount()));
        values.put("max_players", Integer.toString(audienceProvider.maxPlayers()));
        values.put("announcement_id", announcement.id().value());
        values.put("announcement_name", announcement.name());
        
        return PlaceholderContext.of(values);
    }
}
