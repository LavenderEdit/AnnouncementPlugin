package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MessageListMenu {
    private static final int PAGE_SIZE = 45;

    public void open(Player player, EditorSession session, AnnouncementId id, int page) {
        int safePage = Math.max(0, page);
        Announcement announcement = session.draft(id)
                .orElseThrow(() -> new IllegalArgumentException("Draft announcement not found: " + id.value()));
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.MESSAGE_LIST, id, safePage);

        List<String> messages = announcement.messages();
        int totalPages = Math.max(1, (int) Math.ceil((double) messages.size() / PAGE_SIZE));
        String title = "Mensajes - " + id.value() + "  [" + (safePage + 1) + "/" + totalPages + "]";
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.attach(inventory);

        ItemStack[] contents = inventory.getContents();
        MenuItems.fillBorder(contents, Material.GRAY_STAINED_GLASS_PANE);

        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, messages.size());
        for (int index = start; index < end; index++) {
            int slot = index - start;
            holder.mapAnnouncement(slot, AnnouncementId.of(id.value() + ":msg:" + index));
            contents[slot] = messageItem(index, messages.get(index), messages.size());
        }

        if (safePage > 0) {
            contents[45] = MenuItems.item(Material.ARROW, "Anterior");
        }
        contents[47] = MenuItems.glowing(Material.EMERALD, "Agregar mensaje",
                List.of("Anade un mensaje por defecto.",
                        "Edita el YAML para personalizarlo."));
        contents[49] = MenuItems.item(Material.OAK_SIGN, "Volver");
        if (end < messages.size()) {
            contents[53] = MenuItems.item(Material.ARROW, "Siguiente");
        }

        if (messages.isEmpty()) {
            contents[22] = MenuItems.item(Material.BARRIER, "Sin mensajes",
                    List.of("Usa el boton de abajo para agregar uno."));
        }

        MenuItems.fillNavigationRow(contents, PAGE_SIZE);
        inventory.setContents(contents);
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
