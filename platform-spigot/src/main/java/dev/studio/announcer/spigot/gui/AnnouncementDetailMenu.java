package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class AnnouncementDetailMenu {

    public void open(Player player, EditorSession session, AnnouncementId id) {
        Announcement announcement = session.draft(id)
                .orElseThrow(() -> new IllegalArgumentException("Draft announcement not found: " + id.value()));
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.DETAIL, id, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Anuncio - " + id.value());
        holder.attach(inventory);

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
        inventory.setItem(4, MenuItems.item(Material.PAPER, announcement.name(), infoLore));
        inventory.setItem(10, MenuItems.item(
                announcement.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                announcement.enabled() ? "Pausar" : "Activar",
                List.of("Cambia el estado del draft.")));
        inventory.setItem(12, MenuItems.item(Material.ENDER_EYE, "Previsualizar", List.of("Envia este anuncio solo para ti.")));
        inventory.setItem(13, MenuItems.item(Material.WRITABLE_BOOK, "Mensajes",
                List.of("Ver, agregar o eliminar lineas de mensaje.",
                        "Total: " + announcement.messages().size() + " linea(s)")));
        inventory.setItem(14, MenuItems.item(Material.NAME_TAG, "Duplicar", List.of("Crea una copia con sufijo _copy.")));
        inventory.setItem(16, MenuItems.item(Material.REDSTONE_BLOCK, "Eliminar", List.of("Requiere confirmacion.")));
        inventory.setItem(22, MenuItems.item(Material.OAK_SIGN, "Volver"));

        player.openInventory(inventory);
    }
}

