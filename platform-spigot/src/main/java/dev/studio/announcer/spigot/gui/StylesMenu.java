package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class StylesMenu {
    private static final int PAGE_SIZE = 45;

    public void open(Player player, EditorSession session, int page) {
        int safePage = Math.max(0, page);
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.STYLES, null, safePage);

        List<Announcement> announcements = session.drafts();
        int totalPages = Math.max(1, (int) Math.ceil((double) announcements.size() / PAGE_SIZE));
        String title = "Estilos  [" + (safePage + 1) + "/" + totalPages + "]";
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.attach(inventory);

        ItemStack[] contents = inventory.getContents();
        MenuItems.fillBorder(contents, Material.GRAY_STAINED_GLASS_PANE);

        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, announcements.size());
        for (int index = start; index < end; index++) {
            Announcement announcement = announcements.get(index);
            int slot = index - start;
            holder.mapAnnouncement(slot, announcement.id());
            contents[slot] = styleItem(announcement);
        }

        if (safePage > 0) {
            contents[45] = MenuItems.item(Material.ARROW, "Anterior");
        }
        contents[49] = MenuItems.item(Material.OAK_SIGN, "Volver");
        if (end < announcements.size()) {
            contents[53] = MenuItems.item(Material.ARROW, "Siguiente");
        }

        if (announcements.isEmpty()) {
            contents[22] = MenuItems.item(Material.BARRIER, "Sin anuncios",
                    List.of("Crea un anuncio desde",
                            "el menu de Anuncios."));
        }

        MenuItems.fillNavigationRow(contents, PAGE_SIZE);
        inventory.setContents(contents);
        player.openInventory(inventory);
    }

    private org.bukkit.inventory.ItemStack styleItem(Announcement announcement) {
        Material material = materialForStyles(announcement);
        List<String> lore = new ArrayList<>();
        lore.add("ID: " + announcement.id().value());
        lore.add("Canales: " + formatChannels(announcement.channels()));
        if (announcement.soundOptions().isPresent()) {
            lore.add("Sonido: " + announcement.soundOptions().get().key());
        }
        if (announcement.toastOptions().isPresent()) {
            lore.add("Toast: " + announcement.toastOptions().get().title());
        }
        if (announcement.bossBarOptions().isPresent()) {
            lore.add("BossBar: si");
        }
        if (announcement.actionBarOptions().isPresent()) {
            lore.add("ActionBar: si");
        }
        if (announcement.titleOptions().isPresent()) {
            lore.add("Title: si");
        }
        lore.add("Prioridad: " + announcement.priority());
        return MenuItems.item(material, announcement.name(), lore);
    }

    private Material materialForStyles(Announcement announcement) {
        Set<AnnouncementChannel> channels = announcement.channels();
        if (channels.contains(AnnouncementChannel.TITLE) || channels.contains(AnnouncementChannel.SUBTITLE)) {
            return Material.NAME_TAG;
        }
        if (channels.contains(AnnouncementChannel.BOSSBAR)) {
            return Material.DRAGON_HEAD;
        }
        if (channels.contains(AnnouncementChannel.ACTIONBAR)) {
            return Material.OAK_SIGN;
        }
        if (channels.contains(AnnouncementChannel.TOAST)) {
            return Material.DIAMOND;
        }
        if (channels.contains(AnnouncementChannel.SOUND)) {
            return Material.NOTE_BLOCK;
        }
        if (channels.contains(AnnouncementChannel.DISCORD_WEBHOOK)) {
            return Material.PAPER;
        }
        return Material.BOOK;
    }

    private String formatChannels(Set<AnnouncementChannel> channels) {
        return channels.stream()
                .map(channel -> switch (channel) {
                    case CHAT -> "Chat";
                    case TITLE -> "Title";
                    case SUBTITLE -> "Subtitle";
                    case ACTIONBAR -> "ActionBar";
                    case BOSSBAR -> "BossBar";
                    case TOAST -> "Toast";
                    case SOUND -> "Sonido";
                    case DISCORD_WEBHOOK -> "Discord";
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("Ninguno");
    }
}
