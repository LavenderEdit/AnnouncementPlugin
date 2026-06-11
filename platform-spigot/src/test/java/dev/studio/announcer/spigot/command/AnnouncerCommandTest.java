package dev.studio.announcer.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementEditorService;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.PlatformStatusService;
import dev.studio.announcer.application.command.AnnouncerCommandService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class AnnouncerCommandTest {

    @Test
    void editorRejectsConsole() {
        RecordingEditorService editorService = new RecordingEditorService();
        AnnouncerCommand command = new AnnouncerCommand(commandService(), editorService);
        RecordingSender sender = RecordingSender.console("announcer.editor");

        command.onCommand(sender.proxy(CommandSender.class), null, "announcer", new String[] {"editor"});

        assertEquals(List.of("Editor can only be opened in-game."), sender.messages);
        assertEquals(List.of(), editorService.opened);
    }

    @Test
    void editorOpensForPlayerWithEditorPermissionOnly() {
        RecordingEditorService editorService = new RecordingEditorService();
        AnnouncerCommand command = new AnnouncerCommand(commandService(), editorService);
        UUID playerId = UUID.randomUUID();
        RecordingSender sender = RecordingSender.player(playerId, "announcer.editor");

        command.onCommand(sender.proxy(Player.class), null, "announcer", new String[] {"editor"});

        assertEquals(List.of(playerId.toString()), editorService.opened);
    }

    private static AnnouncerCommandService commandService() {
        return new AnnouncerCommandService(new EmptyRepository(), new NoopDispatcher(), new FakeStatus());
    }

    private static final class RecordingSender {
        private final UUID playerId;
        private final List<String> permissions;
        private final List<String> messages = new ArrayList<>();

        private RecordingSender(UUID playerId, List<String> permissions) {
            this.playerId = playerId;
            this.permissions = permissions;
        }

        static RecordingSender console(String... permissions) {
            return new RecordingSender(null, List.of(permissions));
        }

        static RecordingSender player(UUID playerId, String... permissions) {
            return new RecordingSender(playerId, List.of(permissions));
        }

        @SuppressWarnings("unchecked")
        <T> T proxy(Class<T> type) {
            return (T) Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[] {type},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "hasPermission" -> permissions.contains(String.valueOf(args[0]));
                        case "sendMessage" -> {
                            messages.add(String.valueOf(args[0]));
                            yield null;
                        }
                        case "getUniqueId" -> playerId;
                        case "isOp" -> false;
                        case "toString" -> type.getSimpleName() + "Proxy";
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive()) {
                return null;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == void.class) {
                return null;
            }
            return 0;
        }
    }

    private static final class RecordingEditorService implements AnnouncementEditorService {
        private final List<String> opened = new ArrayList<>();

        @Override
        public void openEditor(String audienceId) {
            opened.add(audienceId);
        }
    }

    private static final class EmptyRepository implements AnnouncementRepository {
        @Override
        public Announcement save(Announcement announcement) {
            return announcement;
        }

        @Override
        public Optional<Announcement> findById(AnnouncementId id) {
            return Optional.empty();
        }

        @Override
        public List<Announcement> findAll() {
            return List.of();
        }

        @Override
        public boolean deleteById(AnnouncementId id) {
            return false;
        }
    }

    private static final class NoopDispatcher implements AnnouncementDispatcher {
        @Override
        public DeliverySummary broadcast(Announcement announcement) {
            return new DeliverySummary(0, 0, 0);
        }

        @Override
        public DeliverySummary preview(Announcement announcement, String audienceId) {
            return new DeliverySummary(0, 0, 0);
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
