package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class MessageListMenu {
    private static final int PAGE_SIZE = 45;

    public void open(Player player, EditorSession session, AnnouncementId id, int page) {
        int safePage = Math.max(0, page);
        Announcement announcement = session.draft(id)
                .orElseThrow(() -> new IllegalArgumentException("Draft announcement not found: " + id.value()));
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.MESSAGE_LIST, id, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, "Mensajes - " + id.value());
        holder.attach(inventory);

        List<String> messages = announcement.messages();
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, messages.size());
        for (int index = start; index < end; index++) {
            int slot = index - start;
            holder.mapAnnouncement(slot, AnnouncementId.of(id.value() + ":msg:" + index));
            inventory.setItem(slot, messageItem(index, messages.get(index), messages.size()));
        }

        if (safePage > 0) {
            inventory.setItem(45, MenuItems.item(Material.ARROW, "Anterior"));
        }
        inventory.setItem(47, MenuItems.item(Material.EMERALD, "Agregar mensaje",
                List.of("Anade un mensaje por defecto.",
                        "Edita el YAML para personalizarlo.")));
        inventory.setItem(49, MenuItems.item(Material.OAK_SIGN, "Volver"));
        if (end < messages.size()) {
            inventory.setItem(53, MenuItems.item(Material.ARROW, "Siguiente"));
        }

        if (messages.isEmpty()) {
            inventory.setItem(22, MenuItems.item(Material.BARRIER, "Sin mensajes",
                    List.of("Usa el boton de abajo para agregar uno.")));
        }

        player.openInventory(inventory);
    }

    private org.bukkit.inventory.ItemStack messageItem(int index, String rawMessage, int totalMessages) {
        String preview = stripMiniMessage(rawMessage);
        if (preview.length() > 40) {
            preview = preview.substring(0, 37) + "...";
        }
        List<String> lore = new ArrayList<>();
        lore.add("Linea " + (index + 1) + " de " + totalMessages);
        lore.add("");
        lore.add("Contenido: " + preview);
        lore.add("");
        lore.add("Click izquierdo: eliminar");
        return MenuItems.item(Material.PAPER, "Mensaje #" + (index + 1), lore);
    }

    private String stripMiniMessage(String input) {
        if (input == null) {
            return "(vacio)";
        }
        return input.replaceAll("<[^>]+>", "");
    }
}
