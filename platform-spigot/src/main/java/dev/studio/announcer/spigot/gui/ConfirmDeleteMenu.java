package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ConfirmDeleteMenu {

    public void open(Player player, EditorSession session, AnnouncementId id) {
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.CONFIRM_DELETE, id, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Eliminar " + id.value());
        holder.attach(inventory);

        inventory.setItem(11, MenuItems.item(Material.RED_CONCRETE, "Confirmar", List.of("Elimina el draft del anuncio.")));
        inventory.setItem(15, MenuItems.item(Material.LIME_CONCRETE, "Cancelar", List.of("Vuelve sin eliminar.")));

        player.openInventory(inventory);
    }
}
