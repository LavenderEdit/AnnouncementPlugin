package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.api.message.MessageRenderer;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AnvilTextInputService implements Listener {
    private final MessageRenderer<Component> renderer;
    private final Map<UUID, PendingAnvilInput> pendingInputs = new ConcurrentHashMap<>();

    public AnvilTextInputService(MessageRenderer<Component> renderer) {
        this.renderer = renderer;
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

    public void openRenameAnvil(Player player, AnnouncementId announcementId, Consumer<String> callback) {
        Inventory anvil = Bukkit.createInventory(null, 36, "Renombrar anuncio");
        ItemStack nameInput = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameInput.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(announcementId.value());
            nameInput.setItemMeta(meta);
        }
        anvil.setItem(0, nameInput);
        ItemStack result = new ItemStack(Material.PAPER);
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta != null) {
            resultMeta.setDisplayName("\u00a7aClick para confirmar");
            result.setItemMeta(resultMeta);
        }
        anvil.setItem(2, result);
        pendingInputs.put(player.getUniqueId(), new PendingAnvilInput(announcementId, callback));
        player.openInventory(anvil);
    }

    public void openMessageEditAnvil(Player player, AnnouncementId announcementId, int messageIndex,
                                      String currentMessage, Consumer<String> callback) {
        Inventory anvil = Bukkit.createInventory(null, 36, "Editar mensaje #" + (messageIndex + 1));
        ItemStack nameInput = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameInput.getItemMeta();
        if (meta != null) {
            String preview = currentMessage.replaceAll("<[^>]+>", "");
            if (preview.length() > 30) {
                preview = preview.substring(0, 27) + "...";
            }
            meta.setDisplayName(preview);
            nameInput.setItemMeta(meta);
        }
        anvil.setItem(0, nameInput);
        ItemStack result = new ItemStack(Material.PAPER);
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta != null) {
            resultMeta.setDisplayName("\u00a7aClick para confirmar");
            result.setItemMeta(resultMeta);
        }
        anvil.setItem(2, result);
        pendingInputs.put(player.getUniqueId(), new PendingAnvilInput(announcementId, callback));
        player.openInventory(anvil);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        PendingAnvilInput input = pendingInputs.remove(player.getUniqueId());
        if (input == null) {
            return;
        }
        ItemStack resultItem = event.getInventory().getItem(2);
        if (resultItem != null && resultItem.hasItemMeta()) {
            ItemMeta meta = resultItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String value = meta.getDisplayName();
                if (value != null && !value.isBlank()) {
                    input.callback().accept(value);
                    return;
                }
            }
        }
        input.callback().accept(input.fallback());
    }

    private record PendingAnvilInput(AnnouncementId announcementId, Consumer<String> callback) {
        String fallback() {
            return announcementId.value();
        }
    }
}
