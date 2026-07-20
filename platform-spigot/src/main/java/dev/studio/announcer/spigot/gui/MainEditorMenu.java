package dev.studio.announcer.spigot.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MainEditorMenu {

    public void open(Player player, EditorSession session) {
        EditorMenuHolder holder = new EditorMenuHolder(session, EditorMenuType.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 27, "AdvancedAnnouncer");
        holder.attach(inventory);

        ItemStack[] contents = inventory.getContents();
        MenuItems.fillBorder(contents, Material.GRAY_STAINED_GLASS_PANE);

        contents[10] = MenuItems.glowing(Material.BOOK, "Anuncios",
                java.util.List.of("Ver, editar y duplicar anuncios."));
        contents[11] = MenuItems.item(Material.CLOCK, "Programador",
                java.util.List.of("Intervalos activos desde YAML."));
        contents[13] = MenuItems.item(Material.BELL, "Eventos",
                java.util.List.of("Base preparada para join, rank y donaciones."));
        contents[15] = MenuItems.item(Material.BRUSH, "Estilos",
                java.util.List.of("MiniMessage, sonidos y canales."));
        contents[22] = MenuItems.glowing(Material.EMERALD, "Guardar",
                java.util.List.of("Persiste los cambios en YAML."));
        contents[26] = MenuItems.item(Material.BARRIER, "Cancelar",
                java.util.List.of("Descarta los cambios de esta sesion."));

        inventory.setContents(contents);
        player.openInventory(inventory);
    }
}
