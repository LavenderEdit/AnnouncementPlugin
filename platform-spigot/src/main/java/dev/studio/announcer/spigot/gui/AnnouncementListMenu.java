package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class AnnouncementListMenu {
    private static final int PAGE_SIZE = 45;

    public void open(Player player, EditorSession session, int page) {
        int safePage = Math.max(0, page);
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.LIST, null, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, "AdvancedAnnouncer - Anuncios");
        holder.attach(inventory);

        List<Announcement> announcements = session.drafts();
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, announcements.size());
        for (int index = start; index < end; index++) {
            Announcement announcement = announcements.get(index);
            int slot = index - start;
            holder.mapAnnouncement(slot, announcement.id());
            inventory.setItem(slot, announcementItem(announcement));
        }

        if (safePage > 0) {
            inventory.setItem(45, MenuItems.item(Material.ARROW, "Anterior"));
        }
        inventory.setItem(47, MenuItems.item(Material.EMERALD, "Crear", List.of("Crea un anuncio chat basico.")));
        inventory.setItem(49, MenuItems.item(Material.OAK_SIGN, "Volver"));
        if (end < announcements.size()) {
            inventory.setItem(53, MenuItems.item(Material.ARROW, "Siguiente"));
        }

        player.openInventory(inventory);
    }

    private org.bukkit.inventory.ItemStack announcementItem(Announcement announcement) {
        Material material = announcement.enabled() ? Material.LIME_DYE : Material.GRAY_DYE;
        return MenuItems.item(material, announcement.name(), List.of(
                "ID: " + announcement.id().value(),
                "Canales: " + announcement.channels(),
                "Prioridad: " + announcement.priority(),
                announcement.enabled() ? "Estado: activo" : "Estado: pausado"));
    }
}
