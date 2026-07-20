package dev.studio.announcer.spigot.gui;

import static org.junit.jupiter.api.Assertions.*;

import dev.studio.announcer.common.message.MiniMessageComponentRenderer;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.spigot.adapter.SpigotAudienceProvider;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;

class AnvilTextInputServiceTest {

    @Test
    void validMiniMessageInputPasses() {
        AnvilTextInputService inputService = new AnvilTextInputService(null, new MiniMessageComponentRenderer(), new SpigotAudienceProvider());

        assertTrue(inputService.validateMiniMessage("<green>Hello</green>").valid());
    }

    @Test
    void malformedMiniMessageInputFails() {
        AnvilTextInputService inputService = new AnvilTextInputService(null, new MiniMessageComponentRenderer(), new SpigotAudienceProvider());

        assertFalse(inputService.validateMiniMessage("<green>Hello").valid());
    }

    @Test
    void sanitizesAnnouncementIdsFromTypedInput() {
        AnvilTextInputService inputService = new AnvilTextInputService(null, new MiniMessageComponentRenderer(), new SpigotAudienceProvider());

        assertTrue(inputService.sanitizeAnnouncementId(" Summer Sale! ").value().equals("summer_sale"));
    }

    @Test
    void confirmRenameViaChatExecutesCallbackAndSetsConfirmed() {
        setupMockScheduler();
        AnvilTextInputService service = new AnvilTextInputService(null, new MiniMessageComponentRenderer(), new SpigotAudienceProvider());
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mockPlayer(playerId);
        
        List<String> results = new ArrayList<>();
        service.openRenameAnvil(mockPlayer, AnnouncementId.of("original"), "Original Name", results::add);

        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(true, mockPlayer, "new_name", java.util.Set.of());
        service.onPlayerChat(chatEvent);

        assertEquals(1, results.size());
        assertEquals("new_name", results.get(0));
    }

    @Test
    void chatCancelReturnsFallbackWithoutUpdatingInput() {
        setupMockScheduler();
        AnvilTextInputService service = new AnvilTextInputService(null, new MiniMessageComponentRenderer(), new SpigotAudienceProvider());
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mockPlayer(playerId);

        List<String> results = new ArrayList<>();
        service.openRenameAnvil(mockPlayer, AnnouncementId.of("original_name"), "Original Name", results::add);

        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(true, mockPlayer, "cancelar", java.util.Set.of());
        service.onPlayerChat(chatEvent);

        assertEquals(1, results.size());
        assertEquals("Original Name", results.get(0)); // Returns original fallback
    }

    private Player mockPlayer(UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "openInventory" -> null;
                    case "closeInventory" -> null;
                    case "sendMessage" -> null;
                    default -> null;
                }
        );
    }
    @Test
    void chatEditCancelReturnsFallbackWithoutUpdatingInput() {
        setupMockScheduler();
        AnvilTextInputService service = new AnvilTextInputService(null, new MiniMessageComponentRenderer(), new SpigotAudienceProvider());
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mockPlayer(playerId);

        List<String> results = new ArrayList<>();
        service.openMessageEditAnvil(mockPlayer, AnnouncementId.of("msg1"), 0, "Original message", results::add);

        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(true, mockPlayer, "cancelar", java.util.Set.of());
        service.onPlayerChat(chatEvent);

        assertEquals(1, results.size());
        assertEquals("Original message", results.get(0));
    }

    private static void setupMockScheduler() {
        BukkitScheduler scheduler = (BukkitScheduler) Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[]{BukkitScheduler.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("runTask")) {
                        ((Runnable) args[1]).run();
                        return null;
                    }
                    return null;
                });
        org.bukkit.Server mockServer = (org.bukkit.Server) Proxy.newProxyInstance(
                org.bukkit.Server.class.getClassLoader(),
                new Class<?>[]{org.bukkit.Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getScheduler")) {
                        return scheduler;
                    }
                    return null;
                });
        try {
            Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, mockServer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
