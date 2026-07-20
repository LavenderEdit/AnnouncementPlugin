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

public final class AnnouncementDetailMenu {

    public void open(Player player, EditorSession session, AnnouncementId id) {
        Announcement announcement = session.draft(id)
                .orElseThrow(() -> new IllegalArgumentException("Draft announcement not found: " + id.value()));
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.DETAIL, id, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Anuncio - " + id.value());
        holder.attach(inventory);

        ItemStack[] contents = inventory.getContents();
        MenuItems.fillBorder(contents, Material.GRAY_STAINED_GLASS_PANE);

        List<String> infoLore = new ArrayList<>();
        infoLore.add("ID: " + announcement.id().value());
        infoLore.add("Tipo: " + announcement.type());
        infoLore.add("Canales: " + announcement.channels());
        infoLore.add("Mensajes: " + announcement.messages().size() + " linea(s)");
        if (announcement.interval().isPresent()) {
            infoLore.add("Intervalo: " + announcement.interval().get().getSeconds() + "s");
        }
        if (announcement.cronExpression().isPresent()) {
            infoLore.add("Cron: " + announcement.cronExpression().get());
        }
        if (announcement.permission().isPresent()) {
            infoLore.add("Permiso: " + announcement.permission().get());
        }
        infoLore.add("Prioridad: " + announcement.priority());
        contents[4] = MenuItems.item(Material.PAPER, announcement.name(), infoLore);
        contents[10] = MenuItems.item(
                announcement.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                announcement.enabled() ? "Pausar" : "Activar",
                List.of("Cambia el estado del draft."));
        contents[11] = MenuItems.item(Material.NAME_TAG, "Renombrar",
                List.of("Cambia el nombre del anuncio."));
        contents[12] = MenuItems.item(Material.ENDER_EYE, "Previsualizar",
                List.of("Envia este anuncio solo para ti."));
        contents[13] = MenuItems.item(Material.WRITABLE_BOOK, "Mensajes",
                List.of("Ver, agregar o eliminar lineas de mensaje.",
                        "Total: " + announcement.messages().size() + " linea(s)"));
        contents[14] = MenuItems.item(Material.NAME_TAG, "Duplicar",
                List.of("Crea una copia con sufijo _copy."));
        contents[15] = MenuItems.item(Material.REDSTONE, "Enviar",
                List.of("Envia este anuncio a todos los jugadores."));
        contents[16] = MenuItems.item(Material.REDSTONE_BLOCK, "Eliminar",
                List.of("Requiere confirmacion."));
        contents[22] = MenuItems.item(Material.OAK_SIGN, "Volver");

        inventory.setContents(contents);
        player.openInventory(inventory);
    }
}
