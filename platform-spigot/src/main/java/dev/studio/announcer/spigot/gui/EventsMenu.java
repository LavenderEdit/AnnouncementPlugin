package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class EventsMenu {
    private static final int PAGE_SIZE = 45;

    public void open(Player player, EditorSession session, int page) {
        int safePage = Math.max(0, page);
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.EVENTS, null, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, "AdvancedAnnouncer - Eventos");
        holder.attach(inventory);

        List<Announcement> events = session.drafts().stream()
                .filter(a -> isEvent(a.type()))
                .toList();

        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, events.size());
        for (int index = start; index < end; index++) {
            Announcement announcement = events.get(index);
            int slot = index - start;
            holder.mapAnnouncement(slot, announcement.id());
            inventory.setItem(slot, eventItem(announcement));
        }

        if (safePage > 0) {
            inventory.setItem(45, MenuItems.item(Material.ARROW, "Anterior"));
        }
        inventory.setItem(49, MenuItems.item(Material.OAK_SIGN, "Volver"));
        if (end < events.size()) {
            inventory.setItem(53, MenuItems.item(Material.ARROW, "Siguiente"));
        }

        if (events.isEmpty()) {
            inventory.setItem(22, MenuItems.item(Material.BARRIER, "Sin anuncios de eventos",
                    List.of("Crea un anuncio con tipo EVENT_*",
                            "desde el editor de anuncios.")));
        }

        player.openInventory(inventory);
    }

    private org.bukkit.inventory.ItemStack eventItem(Announcement announcement) {
        Material material = announcement.enabled() ? Material.BELL : Material.SOUL_LANTERN;
        List<String> lore = new ArrayList<>();
        lore.add("ID: " + announcement.id().value());
        lore.add("Evento: " + formatType(announcement.type()));
        if (!announcement.conditions().isEmpty()) {
            lore.add("Condiciones: " + String.join(", ", announcement.conditions()));
        }
        lore.add("Prioridad: " + announcement.priority());
        lore.add(announcement.enabled() ? "Estado: activo" : "Estado: pausado");
        return MenuItems.item(material, announcement.name(), lore);
    }

    private boolean isEvent(AnnouncementType type) {
        return switch (type) {
            case EVENT_JOIN, EVENT_QUIT, EVENT_DEATH, EVENT_WORLD_CHANGE, EVENT_COMMAND -> true;
            default -> false;
        };
    }

    private String formatType(AnnouncementType type) {
        return switch (type) {
            case EVENT_JOIN -> "Unirse al servidor";
            case EVENT_QUIT -> "Desconectar";
            case EVENT_DEATH -> "Muerte";
            case EVENT_WORLD_CHANGE -> "Cambio de mundo";
            case EVENT_COMMAND -> "Comando";
            default -> type.name();
        };
    }
}
