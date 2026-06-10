package dev.studio.announcer.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.PlatformStatusService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    }

    @Test
    void redisAndDiscordTestAreExplicitlyDisabledInPhaseTwo() {
        AnnouncerCommandService service = service(new FakeRepository(), new FakeDispatcher());

        assertEquals(CommandOutcome.success("Redis is disabled or not configured in this phase."),
                service.handle(CommandRequest.console("redis", "test")));
        assertEquals(CommandOutcome.success("Discord is disabled or not configured in this phase."),
                service.handle(CommandRequest.console("discord", "test")));
    }

    private static AnnouncerCommandService service(FakeRepository repository, FakeDispatcher dispatcher) {
        return new AnnouncerCommandService(repository, dispatcher, new FakeStatus());
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
}
