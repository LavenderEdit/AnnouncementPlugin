package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class AnnouncementListMenu {
    private static final int PAGE_SIZE = 45;

    public void open(Player player, EditorSession session, int page) {
        int safePage = Math.max(0, page);
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.LIST, null, safePage);
        int totalPages = Math.max(1, (int) Math.ceil((double) session.drafts().size() / PAGE_SIZE));
        String title = "Anuncios  [" + (safePage + 1) + "/" + totalPages + "]";
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.attach(inventory);

        ItemStack[] contents = inventory.getContents();
        MenuItems.fillBorder(contents, Material.GRAY_STAINED_GLASS_PANE);

        List<Announcement> announcements = session.drafts();
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, announcements.size());
        for (int index = start; index < end; index++) {
            Announcement announcement = announcements.get(index);
            int slot = index - start;
            holder.mapAnnouncement(slot, announcement.id());
            contents[slot] = announcementItem(announcement);
        }

        if (safePage > 0) {
            contents[45] = MenuItems.item(Material.ARROW, "Anterior");
        }
        contents[47] = MenuItems.glowing(Material.EMERALD, "Crear",
                List.of("Crea un anuncio chat basico."));
        contents[49] = MenuItems.item(Material.OAK_SIGN, "Volver");
        if (end < announcements.size()) {
            contents[53] = MenuItems.item(Material.ARROW, "Siguiente");
        }

        MenuItems.fillNavigationRow(contents, PAGE_SIZE);
        inventory.setContents(contents);
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
