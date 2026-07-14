package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class ConfirmDeleteMenu {

    public void open(Player player, EditorSession session, AnnouncementId id) {
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.CONFIRM_DELETE, id, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Eliminar " + id.value());
        holder.attach(inventory);

        ItemStack[] contents = inventory.getContents();
        MenuItems.fillBorder(contents, Material.RED_STAINED_GLASS_PANE);

        contents[4] = MenuItems.item(Material.BARRIER, "Eliminar anuncio",
                List.of("Estas seguro de eliminar " + id.value() + "?",
                        "Esta accion no se puede deshacer."));
        contents[11] = MenuItems.glowing(Material.RED_CONCRETE, "Confirmar",
                List.of("Elimina el draft del anuncio."));
        contents[15] = MenuItems.item(Material.LIME_CONCRETE, "Cancelar",
                List.of("Vuelve sin eliminar."));

        inventory.setContents(contents);
        player.openInventory(inventory);
    }
}
