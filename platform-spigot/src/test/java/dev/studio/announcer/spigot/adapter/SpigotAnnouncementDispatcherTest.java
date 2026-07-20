package dev.studio.announcer.spigot.adapter;

import static org.junit.jupiter.api.Assertions.*;

import dev.studio.announcer.api.audience.Audience;
import dev.studio.announcer.api.audience.OthersAudience;
import dev.studio.announcer.api.audience.PlayerAudience;
import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.SoundService;
import dev.studio.announcer.api.service.ToastNotificationService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.validation.ValidationResult;
import dev.studio.announcer.spigot.service.PriorityActionBarService;
import dev.studio.announcer.spigot.service.PriorityBossBarService;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class SpigotAnnouncementDispatcherTest {

    @Test
    void playerAudienceWithInvalidOrOfflinePlayerReturnsErrorSummary() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hello"))
                .build();

        // Offline UUID
        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.player(UUID.randomUUID().toString()), null);
        assertEquals(0, summary.delivered());
        assertEquals(0, summary.skippedByPermission());
        assertEquals(1, summary.invalid()); // Handled safely, counted as 1 error/invalid

        // Offline Name
        DeliverySummary summary2 = dispatcher.dispatch(announcement, Audience.player("OfflinePlayer"), null);
        assertEquals(0, summary2.delivered());
        assertEquals(0, summary2.skippedByPermission());
        assertEquals(1, summary2.invalid());
    }

    @Test
    void playerAudienceResolvesByPlayerNameFallback() {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mockPlayer(playerId, "Alice");

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(mockPlayer);

        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hello"))
                .build();

        // Resolve by name fallback
        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.player("Alice"), null);
        assertEquals(1, summary.delivered());
        assertEquals(0, summary.skippedByPermission());
        assertEquals(0, summary.invalid());
    }

    @Test
    void othersAudienceWithoutActorDeliversToAllPlayers() {
        Player mockPlayer1 = mockPlayer(UUID.randomUUID(), "Alice");
        Player mockPlayer2 = mockPlayer(UUID.randomUUID(), "Bob");

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(mockPlayer1);
        provider.addPlayer(mockPlayer2);

        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hello"))
                .build();

        // OthersAudience with null actorId
        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.others(), null);
        assertEquals(2, summary.delivered()); // Delivers to both players since no actor is excluded
        assertEquals(0, summary.skippedByPermission());
        assertEquals(0, summary.invalid());
    }

    @Test
    void othersAudienceExcludesActorCorrectly() {
        UUID aliceId = UUID.randomUUID();
        Player mockPlayer1 = mockPlayer(aliceId, "Alice");
        Player mockPlayer2 = mockPlayer(UUID.randomUUID(), "Bob");

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(mockPlayer1);
        provider.addPlayer(mockPlayer2);

        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hello"))
                .build();

        // OthersAudience excluding Alice
        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.others(), aliceId.toString());
        assertEquals(1, summary.delivered()); // Bob gets it
        assertEquals(1, summary.skippedByPermission()); // Alice is skipped (actor)
        assertEquals(0, summary.invalid());
    }

    @Test
    void actorAudienceWithNullActorIdReturnsEmptySummary() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hello"))
                .build();

        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.actor(), null);
        assertEquals(0, summary.delivered());
        assertEquals(0, summary.skippedByPermission());
        assertEquals(0, summary.invalid());
    }

    @Test
    void actorAudienceWithOfflineActorIdReturnsEmptySummary() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Hello"))
                .build();

        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.actor(), "offline-uuid");
        assertEquals(0, summary.delivered());
        assertEquals(0, summary.skippedByPermission());
        assertEquals(0, summary.invalid());
    }

    @Test
    void playerAudienceWithBlankPlayerIdThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Audience.player(""));
        assertThrows(IllegalArgumentException.class, () -> Audience.player("   "));
    }

    @Test
    void resolvesActorAndViewerPlaceholdersCorrectly() {
        UUID aliceId = UUID.randomUUID();
        Player mockPlayer1 = mockPlayer(aliceId, "Alice");
        UUID bobId = UUID.randomUUID();
        Player mockPlayer2 = mockPlayer(bobId, "Bob");

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(mockPlayer1);
        provider.addPlayer(mockPlayer2);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Actor: {actor_name}, Viewer: {viewer_name}, Legacy: {player_name}"))
                .build();

        // Deliver to Bob (viewer) with Alice as actor
        dispatcher.dispatch(announcement, Audience.player(bobId.toString()), aliceId.toString());

        assertEquals(1, sent.size());
        String content = ((net.kyori.adventure.text.TextComponent) sent.get(0)).content();
        assertEquals("Actor: Alice, Viewer: Bob, Legacy: Bob", content);
    }

    @Test
    void resolvesNullActorPlaceholdersToEmptyString() {
        UUID bobId = UUID.randomUUID();
        Player mockPlayer2 = mockPlayer(bobId, "Bob");

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(mockPlayer2);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        Announcement announcement = Announcement.builder(AnnouncementId.of("id"), "Test")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Actor: '{actor_name}', Viewer: {viewer_name}"))
                .build();

        // Deliver to Bob (viewer) with null actor
        dispatcher.dispatch(announcement, Audience.player(bobId.toString()), null);

        assertEquals(1, sent.size());
        String content = ((net.kyori.adventure.text.TextComponent) sent.get(0)).content();
        assertEquals("Actor: '', Viewer: Bob", content);
    }

    private SpigotAnnouncementDispatcher createDispatcher(SpigotAudienceProvider provider) {
        return createDispatcher(provider, new ArrayList<>());
    }

    private SpigotAnnouncementDispatcher createDispatcher(SpigotAudienceProvider provider, List<Component> sentComponents) {
        net.kyori.adventure.audience.Audience mockAudience = (net.kyori.adventure.audience.Audience) Proxy.newProxyInstance(
                net.kyori.adventure.audience.Audience.class.getClassLoader(),
                new Class<?>[]{net.kyori.adventure.audience.Audience.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage")) {
                        sentComponents.add((Component) args[0]);
                    }
                    return null;
                }
        );

        BukkitAudiences mockAudiences = (BukkitAudiences) Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[]{BukkitAudiences.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("player")) {
                        return mockAudience;
                    }
                    return null;
                }
        );

        MessageRenderer<Component> renderer = new MessageRenderer<>() {
            @Override public ValidationResult validate(String input) { return ValidationResult.ok(); }
            @Override public Component render(String input, PlaceholderContext context) {
                // Perform placeholder replacements for testing
                String resolved = input;
                if (context != null) {
                    for (Map.Entry<String, String> entry : context.values().entrySet()) {
                        resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
                    }
                }
                return Component.text(resolved);
            }
        };

        return new SpigotAnnouncementDispatcher(
                mockAudiences,
                renderer,
                provider,
                null,
                null,
                null,
                null,
                Set.of()
        );
    }

    @Test
    void joinStateConditionFirstJoinEvaluatesCorrectly() {
        UUID playerId = UUID.randomUUID();
        Player newPlayer = mockPlayer(playerId, "Newbie", false); // hasPlayedBefore = false -> FIRST_JOIN

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(newPlayer);

        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);
        
        Announcement firstJoinAnn = Announcement.builder(AnnouncementId.of("id1"), "First Join")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Welcome first time!"))
                .conditions(List.of("join-state: FIRST_JOIN"))
                .build();

        Announcement recurringAnn = Announcement.builder(AnnouncementId.of("id2"), "Recurring Join")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Welcome back!"))
                .conditions(List.of("join-state: RECURRING"))
                .build();

        // Dispatch for new player
        DeliverySummary summary1 = dispatcher.dispatch(firstJoinAnn, Audience.player(playerId.toString()), playerId.toString());
        assertEquals(1, summary1.delivered());

        DeliverySummary summary2 = dispatcher.dispatch(recurringAnn, Audience.player(playerId.toString()), playerId.toString());
        assertEquals(0, summary2.delivered()); // Skipped because player is FIRST_JOIN but condition requires RECURRING
    }

    @Test
    void joinStatePlaceholderResolvesToFirstJoin() {
        UUID playerId = UUID.randomUUID();
        Player newPlayer = mockPlayer(playerId, "Newbie", false); // hasPlayedBefore = false -> FIRST_JOIN

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(newPlayer);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);

        Announcement announcement = Announcement.builder(AnnouncementId.of("id1"), "First Join")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("State: {join_state}"))
                .build();

        dispatcher.dispatch(announcement, Audience.player(playerId.toString()), playerId.toString());

        assertEquals(1, sent.size());
        String content = ((net.kyori.adventure.text.TextComponent) sent.get(0)).content();
        assertEquals("State: FIRST_JOIN", content);
    }

    @Test
    void joinStatePlaceholderResolvesToRecurring() {
        UUID playerId = UUID.randomUUID();
        Player returningPlayer = mockPlayer(playerId, "Regular", true); // hasPlayedBefore = true -> RECURRING

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(returningPlayer);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);

        Announcement announcement = Announcement.builder(AnnouncementId.of("id1"), "Recurring Join")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("State: {join_state}"))
                .build();

        dispatcher.dispatch(announcement, Audience.player(playerId.toString()), playerId.toString());

        assertEquals(1, sent.size());
        String content = ((net.kyori.adventure.text.TextComponent) sent.get(0)).content();
        assertEquals("State: RECURRING", content);
    }

    @Test
    void joinStateConditionRecurringEvaluatesCorrectly() {
        UUID playerId = UUID.randomUUID();
        Player returningPlayer = mockPlayer(playerId, "Regular", true); // hasPlayedBefore = true -> RECURRING

        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        provider.addPlayer(returningPlayer);

        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider);

        Announcement firstJoinAnn = Announcement.builder(AnnouncementId.of("id1"), "First Join")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Welcome first time!"))
                .conditions(List.of("join-state: FIRST_JOIN"))
                .build();

        Announcement recurringAnn = Announcement.builder(AnnouncementId.of("id2"), "Recurring Join")
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of("Welcome back!"))
                .conditions(List.of("join-state: RECURRING"))
                .build();

        // Dispatch for returning player
        DeliverySummary summary1 = dispatcher.dispatch(firstJoinAnn, Audience.player(playerId.toString()), playerId.toString());
        assertEquals(0, summary1.delivered()); // Skipped because player is RECURRING but condition requires FIRST_JOIN

        DeliverySummary summary2 = dispatcher.dispatch(recurringAnn, Audience.player(playerId.toString()), playerId.toString());
        assertEquals(1, summary2.delivered());
    }
    private Player mockPlayer(UUID uuid, String name) {
        return mockPlayer(uuid, name, true);
    }

    private Player mockPlayer(UUID uuid, String name, boolean playedBefore) {
        World mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) {
                        return "world";
                    }
                    return null;
                }
        );
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getName" -> name;
                    case "getDisplayName" -> name;
                    case "getWorld" -> mockWorld;
                    case "hasPlayedBefore" -> playedBefore;
                    case "hasPermission" -> true;
                    case "hashCode" -> uuid.hashCode();
                    case "equals" -> args[0] != null && args[0].getClass() == proxy.getClass() && uuid.equals(((Player) args[0]).getUniqueId());
                    default -> null;
                }
        );
    }

    private static final class FakeSpigotAudienceProvider extends SpigotAudienceProvider {
        private final Map<String, Player> players = new HashMap<>();

        void addPlayer(Player player) {
            players.put(player.getUniqueId().toString(), player);
            players.put(player.getName(), player);
        }

        @Override
        public Collection<? extends Player> onlinePlayers() {
            return List.copyOf(players.values()).stream()
                    .distinct() // remove duplicates from mapping both by UUID and name
                    .toList();
        }

        @Override
        public Optional<Player> findPlayer(String audienceId) {
            if (audienceId == null || audienceId.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(players.get(audienceId));
        }
    }
}
