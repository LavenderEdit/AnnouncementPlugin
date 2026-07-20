package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import dev.studio.announcer.spigot.adapter.SpigotAudienceProvider;

public final class AnvilTextInputService implements Listener {
    private final JavaPlugin plugin;
    private final MessageRenderer<Component> renderer;
    private final SpigotAudienceProvider audienceProvider;
    private final Map<UUID, PendingInput> pendingAnvilInputs = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInput> pendingChatInputs = new ConcurrentHashMap<>();

    public AnvilTextInputService(JavaPlugin plugin, MessageRenderer<Component> renderer, SpigotAudienceProvider audienceProvider) {
        this.plugin = plugin;
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.audienceProvider = Objects.requireNonNull(audienceProvider, "audienceProvider");
    }

    public ValidationResult validateMiniMessage(String input) {
        return renderer.validate(input);
    }

    public AnnouncementId sanitizeAnnouncementId(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(java.util.Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9_-]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "announcement";
        }
        return AnnouncementId.of(normalized);
    }

    public void openRenameAnvil(Player player, AnnouncementId announcementId, String currentName, Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("=== RENOMBRAR ANUNCIO ===").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Nombre actual: " + currentName).color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Escribe el nuevo nombre en el chat y presiona Enter.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Escribe 'cancelar' para volver sin realizar cambios.").color(NamedTextColor.RED));
        player.sendMessage(Component.text(""));

        pendingChatInputs.put(player.getUniqueId(), new PendingInput(currentName, callback));
    }

    public void openMessageEditAnvil(Player player, AnnouncementId announcementId, int messageIndex,
                                      String currentMessage, Consumer<String> callback) {
        // Use chat capture for editing long messages
        player.closeInventory();
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("=== EDITAR MENSAJE ===").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Escribe el nuevo mensaje en el chat y presiona Enter.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Puedes utilizar tags de MiniMessage y caracteres Unicode (tildes, símbolos, etc.).").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Escribe 'cancelar' para volver sin realizar cambios.").color(NamedTextColor.RED));
        player.sendMessage(Component.text(""));

        pendingChatInputs.put(player.getUniqueId(), new PendingInput(currentMessage, callback));
    }

    void triggerAnvilConfirmForTest(Player player, Inventory top) {
        PendingInput input = pendingAnvilInputs.get(player.getUniqueId());
        if (input != null) {
            handleAnvilClickSlot2(player, top, input);
        }
    }

    void handleAnvilClickSlot2(Player player, Inventory top, PendingInput input) {
        String value = audienceProvider.getAnvilRenameText(top);
        if (value != null && !value.isEmpty()) {
            input.confirm();
            input.callback().accept(value);
            player.closeInventory();
            return;
        }
        // If empty or invalid, treat as cancel or keep original
        input.confirm(); // set to confirmed to prevent double-firing close event
        input.callback().accept(input.fallback());
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top instanceof org.bukkit.inventory.AnvilInventory)) {
            return;
        }
        PendingInput input = pendingAnvilInputs.get(player.getUniqueId());
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == 2) {
            handleAnvilClickSlot2(player, top, input);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (event.getInventory() instanceof org.bukkit.inventory.AnvilInventory anvilInventory) {
            anvilInventory.setRepairCost(0);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        PendingInput input = pendingAnvilInputs.remove(player.getUniqueId());
        if (input != null && !input.confirmed()) {
            // Cancelled via closing GUI, execute callback with fallback to reopen menu
            input.confirm();
            input.callback().accept(input.fallback());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput input = pendingChatInputs.get(player.getUniqueId());
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancelar") || message.equalsIgnoreCase("cancel")) {
            pendingChatInputs.remove(player.getUniqueId());
            player.sendMessage(Component.text("Edición cancelada.").color(NamedTextColor.RED));
            // Reopen GUI by executing callback with original value on primary thread
            Bukkit.getScheduler().runTask(plugin, () -> input.callback().accept(input.fallback()));
            return;
        }

        // Validate MiniMessage syntax
        ValidationResult validation = validateMiniMessage(message);
        if (!validation.valid()) {
            player.sendMessage(Component.text("Mensaje MiniMessage inválido: " + String.join(", ", validation.errors())).color(NamedTextColor.RED));
            player.sendMessage(Component.text("Por favor, vuelve a intentarlo o escribe 'cancelar'."));
            return;
        }

        pendingChatInputs.remove(player.getUniqueId());
        // Execute callback with new message on primary thread
        Bukkit.getScheduler().runTask(plugin, () -> input.callback().accept(message));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingAnvilInputs.remove(event.getPlayer().getUniqueId());
        pendingChatInputs.remove(event.getPlayer().getUniqueId());
    }

    public void clearAll() {
        pendingAnvilInputs.clear();
        pendingChatInputs.clear();
    }

    private static final class PendingInput {
        private final String fallback;
        private final Consumer<String> callback;
        private boolean confirmed;

        PendingInput(String fallback, Consumer<String> callback) {
            this.fallback = fallback;
            this.callback = callback;
        }

        String fallback() {
            return fallback;
        }

        Consumer<String> callback() {
            return callback;
        }

        boolean confirmed() {
            return confirmed;
        }

        void confirm() {
            this.confirmed = true;
        }
    }
}
