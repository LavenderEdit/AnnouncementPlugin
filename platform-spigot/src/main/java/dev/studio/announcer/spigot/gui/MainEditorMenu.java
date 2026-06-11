package dev.studio.announcer.spigot.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class MainEditorMenu {

    public void open(Player player, EditorSession session) {
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 27, "AdvancedAnnouncer");
        holder.attach(inventory);

        inventory.setItem(10, MenuItems.item(Material.BOOK, "Anuncios", java.util.List.of("Ver, editar y duplicar anuncios.")));
        inventory.setItem(11, MenuItems.item(Material.CLOCK, "Programador", java.util.List.of("Intervalos activos desde YAML.")));
        inventory.setItem(13, MenuItems.item(Material.BELL, "Eventos", java.util.List.of("Base preparada para join, rank y donaciones.")));
        inventory.setItem(15, MenuItems.item(Material.BRUSH, "Estilos", java.util.List.of("MiniMessage, sonidos y canales.")));
        inventory.setItem(22, MenuItems.item(Material.EMERALD, "Guardar", java.util.List.of("Persiste los cambios en YAML.")));
        inventory.setItem(26, MenuItems.item(Material.BARRIER, "Cancelar", java.util.List.of("Descarta los cambios de esta sesion.")));

        player.openInventory(inventory);
    }
}
