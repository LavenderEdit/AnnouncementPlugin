package dev.studio.announcer.spigot.adapter;

import static org.junit.jupiter.api.Assertions.*;

import dev.studio.announcer.api.audience.Audience;
import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.api.placeholder.PlaceholderContext;
import dev.studio.announcer.api.service.AnnouncementExecutionContext;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.ScheduledTask;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.application.usecase.ExecuteTriggeredAnnouncementsUseCase;
import dev.studio.announcer.application.usecase.TriggerMatcher;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementType;
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

/**
 * Flujo completo de bienvenida en el runtime de Spigot: el caso de uso programa los
 * anuncios EVENT_JOIN habilitados y el dispatcher real evalua la condicion de primera
 * entrada ({@code join-state}) usando {@code Player.hasPlayedBefore()}, resolviendo la
 * audiencia desde los metadatos del anuncio.
 */
class SpigotJoinWelcomeFlowTest {

    private static Announcement joinAnnouncement(String id, String message, String joinStateCondition, String audience, boolean enabled) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("audience", audience);
        return Announcement.builder(AnnouncementId.of(id), id)
                .type(AnnouncementType.EVENT_JOIN)
                .enabled(enabled)
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(List.of(message))
                .conditions(joinStateCondition == null ? List.of() : List.of(joinStateCondition))
                .metadata(metadata)
                .build();
    }

    @Test
    void newPlayerTriggersOnlyFirstJoinAnnouncement() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        UUID playerId = UUID.randomUUID();
        Player newPlayer = mockPlayer(playerId, "Newbie", false);
        provider.addPlayer(newPlayer);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        InMemoryRepository repository = new InMemoryRepository(
                joinAnnouncement("first_join", "FIRST", "join-state: FIRST_JOIN", "all", true),
                joinAnnouncement("recurring", "RECURRING", "join-state: RECURRING", "all", true));

        ExecuteTriggeredAnnouncementsUseCase useCase = new ExecuteTriggeredAnnouncementsUseCase(
                repository, dispatcher, new ImmediateScheduler(), new TriggerMatcher("server-01", Set.of("default")), Duration.ZERO);

        useCase.execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext(playerId.toString(), Map.of()));

        long first = sent.stream().filter(c -> "FIRST".equals(text(c))).count();
        long recurring = sent.stream().filter(c -> "RECURRING".equals(text(c))).count();
        assertEquals(1, first, "new player should receive first_join");
        assertEquals(0, recurring, "new player should NOT receive recurring");
    }

    @Test
    void returningPlayerTriggersOnlyRecurringAnnouncement() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        UUID playerId = UUID.randomUUID();
        Player returningPlayer = mockPlayer(playerId, "Regular", true);
        provider.addPlayer(returningPlayer);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        InMemoryRepository repository = new InMemoryRepository(
                joinAnnouncement("first_join", "FIRST", "join-state: FIRST_JOIN", "all", true),
                joinAnnouncement("recurring", "RECURRING", "join-state: RECURRING", "all", true));

        ExecuteTriggeredAnnouncementsUseCase useCase = new ExecuteTriggeredAnnouncementsUseCase(
                repository, dispatcher, new ImmediateScheduler(), new TriggerMatcher("server-01", Set.of("default")), Duration.ZERO);

        useCase.execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext(playerId.toString(), Map.of()));

        long first = sent.stream().filter(c -> "FIRST".equals(text(c))).count();
        long recurring = sent.stream().filter(c -> "RECURRING".equals(text(c))).count();
        assertEquals(0, first, "returning player should NOT receive first_join");
        assertEquals(1, recurring, "returning player should receive recurring");
    }

    @Test
    void disabledJoinAnnouncementIsNotDelivered() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        UUID playerId = UUID.randomUUID();
        provider.addPlayer(mockPlayer(playerId, "Newbie", false));

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        InMemoryRepository repository = new InMemoryRepository(
                joinAnnouncement("first_join", "FIRST", "join-state: FIRST_JOIN", "all", false));

        ExecuteTriggeredAnnouncementsUseCase useCase = new ExecuteTriggeredAnnouncementsUseCase(
                repository, dispatcher, new ImmediateScheduler(), new TriggerMatcher("server-01", Set.of("default")), Duration.ZERO);

        useCase.execute(AnnouncementType.EVENT_JOIN, new AnnouncementExecutionContext(playerId.toString(), Map.of()));

        assertTrue(sent.isEmpty());
    }

    @Test
    void othersAudienceDeliversToEveryoneExceptActor() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        UUID actorId = UUID.randomUUID();
        Player actor = mockPlayer(actorId, "Actor", true);
        Player other = mockPlayer(UUID.randomUUID(), "Other", true);
        provider.addPlayer(actor);
        provider.addPlayer(other);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        Announcement announcement = joinAnnouncement("welcome", "HI", null, "others", true);

        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.others(), actorId.toString());

        assertEquals(1, summary.delivered());
        assertEquals(1, summary.skippedByPermission()); // actor excluded
        assertEquals(1, sent.size());
    }

    @Test
    void actorAudienceDeliversOnlyToActor() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        UUID actorId = UUID.randomUUID();
        Player actor = mockPlayer(actorId, "Actor", true);
        Player other = mockPlayer(UUID.randomUUID(), "Other", true);
        provider.addPlayer(actor);
        provider.addPlayer(other);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        Announcement announcement = joinAnnouncement("welcome", "HI", null, "actor", true);

        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.actor(), actorId.toString());

        assertEquals(1, summary.delivered());
        assertEquals(0, summary.skippedByPermission());
        assertEquals(1, sent.size()); // only the actor received the message
    }

    @Test
    void playerAudienceDeliversOnlyToTargetPlayer() {
        FakeSpigotAudienceProvider provider = new FakeSpigotAudienceProvider();
        UUID targetId = UUID.randomUUID();
        Player target = mockPlayer(targetId, "Target", true);
        Player other = mockPlayer(UUID.randomUUID(), "Other", true);
        provider.addPlayer(target);
        provider.addPlayer(other);

        List<Component> sent = new ArrayList<>();
        SpigotAnnouncementDispatcher dispatcher = createDispatcher(provider, sent);
        Announcement announcement = joinAnnouncement("welcome", "HI", null, "all", true);

        // Deliver to a specific player regardless of who triggered the join
        DeliverySummary summary = dispatcher.dispatch(announcement, Audience.player(targetId.toString()), "some-actor");

        assertEquals(1, summary.delivered());
        assertEquals(1, sent.size());
    }

    private static String text(Component component) {
        return ((net.kyori.adventure.text.TextComponent) component).content();
    }

    private SpigotAnnouncementDispatcher createDispatcher(SpigotAudienceProvider provider, List<Component> sent) {
        net.kyori.adventure.audience.Audience mockAudience = (net.kyori.adventure.audience.Audience) Proxy.newProxyInstance(
                net.kyori.adventure.audience.Audience.class.getClassLoader(),
                new Class<?>[]{net.kyori.adventure.audience.Audience.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage")) {
                        sent.add((Component) args[0]);
                    }
                    return null;
                });

        BukkitAudiences mockAudiences = (BukkitAudiences) Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[]{BukkitAudiences.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("player")) {
                        return mockAudience;
                    }
                    return null;
                });

        MessageRenderer<Component> renderer = new MessageRenderer<>() {
            @Override public ValidationResult validate(String input) { return ValidationResult.ok(); }
            @Override public Component render(String input, PlaceholderContext context) { return Component.text(input); }
        };

        return new SpigotAnnouncementDispatcher(
                mockAudiences, renderer, provider, null, null, null, null, Set.of());
    }

    private Player mockPlayer(UUID uuid, String name, boolean playedBefore) {
        World mockWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "world";
                    return null;
                });
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
                });
    }

    private static final class InMemoryRepository implements AnnouncementRepository {
        private final Map<AnnouncementId, Announcement> store = new HashMap<>();
        private final List<Announcement> all;

        InMemoryRepository(Announcement... announcements) {
            for (Announcement a : announcements) {
                store.put(a.id(), a);
            }
            this.all = List.of(announcements);
        }

        @Override public Announcement save(Announcement a) { store.put(a.id(), a); return a; }
        @Override public Optional<Announcement> findById(AnnouncementId id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Announcement> findAll() { return all; }
        @Override public boolean deleteById(AnnouncementId id) { return store.remove(id) != null; }
    }

    private static final class ImmediateScheduler implements SchedulerPort {
        @Override
        public ScheduledTask scheduleOnce(String taskId, Duration delay, Runnable task) {
            task.run();
            return new ScheduledTask() {
                @Override public String id() { return taskId; }
                @Override public boolean cancelled() { return false; }
                @Override public void close() {}
            };
        }

        @Override
        public ScheduledTask scheduleRepeating(String taskId, Duration initialDelay, Duration interval, Runnable task) {
            return new ScheduledTask() {
                @Override public String id() { return taskId; }
                @Override public boolean cancelled() { return false; }
                @Override public void close() {}
            };
        }
    }

    private static final class FakeSpigotAudienceProvider extends SpigotAudienceProvider {
        private final Map<String, Player> players = new HashMap<>();

        void addPlayer(Player player) {
            players.put(player.getUniqueId().toString(), player);
            players.put(player.getName(), player);
        }

        @Override
        public Collection<? extends Player> onlinePlayers() {
            return List.copyOf(players.values()).stream().distinct().toList();
        }

        @Override
        public Optional<Player> findPlayer(String audienceId) {
            if (audienceId == null || audienceId.isBlank()) return Optional.empty();
            return Optional.ofNullable(players.get(audienceId));
        }
    }
}
