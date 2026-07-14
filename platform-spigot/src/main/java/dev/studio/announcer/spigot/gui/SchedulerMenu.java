package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SchedulerMenu {
    private static final int PAGE_SIZE = 45;

    public void open(Player player, EditorSession session, int page) {
        int safePage = Math.max(0, page);
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.SCHEDULER, null, safePage);

        List<Announcement> scheduled = session.drafts().stream()
                .filter(a -> a.interval().isPresent() || a.cronExpression().isPresent())
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil((double) scheduled.size() / PAGE_SIZE));
        String title = "Programador  [" + (safePage + 1) + "/" + totalPages + "]";
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.attach(inventory);

        ItemStack[] contents = inventory.getContents();
        MenuItems.fillBorder(contents, Material.GRAY_STAINED_GLASS_PANE);

        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, scheduled.size());
        for (int index = start; index < end; index++) {
            Announcement announcement = scheduled.get(index);
            int slot = index - start;
            holder.mapAnnouncement(slot, announcement.id());
            contents[slot] = scheduleItem(announcement);
        }

        if (safePage > 0) {
            contents[45] = MenuItems.item(Material.ARROW, "Anterior");
        }
        contents[49] = MenuItems.item(Material.OAK_SIGN, "Volver");
        if (end < scheduled.size()) {
            contents[53] = MenuItems.item(Material.ARROW, "Siguiente");
        }

        if (scheduled.isEmpty()) {
            contents[22] = MenuItems.item(Material.BARRIER, "Sin anuncios programados",
                    List.of("Crea un anuncio con intervalo o cron",
                            "desde el menu de Anuncios."));
        }

        MenuItems.fillNavigationRow(contents, PAGE_SIZE);
        inventory.setContents(contents);
        player.openInventory(inventory);
    }

    private org.bukkit.inventory.ItemStack scheduleItem(Announcement announcement) {
        Material material = announcement.enabled() ? Material.CLOCK : Material.COMPARATOR;
        List<String> lore = new ArrayList<>();
        lore.add("ID: " + announcement.id().value());
        lore.add("Tipo: " + announcement.type());
        announcement.interval().ifPresent(interval -> lore.add("Intervalo: " + formatDuration(interval)));
        announcement.cronExpression().ifPresent(cron -> lore.add("Cron: " + cron));
        lore.add("Prioridad: " + announcement.priority());
        lore.add(announcement.enabled() ? "Estado: activo" : "Estado: pausado");
        return MenuItems.item(material, announcement.name(), lore);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
}
