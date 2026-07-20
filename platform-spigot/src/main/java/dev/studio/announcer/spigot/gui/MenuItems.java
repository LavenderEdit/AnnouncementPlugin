package dev.studio.announcer.spigot.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MenuItems {
    private MenuItems() {
    }

    static ItemStack item(Material material, String name) {
        return item(material, name, List.of());
    }

    static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack glowing(Material material, String name, List<String> lore) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack glassPane() {
        return new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    }

    static ItemStack separator() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    static List<String> section(String header, String... lines) {
        List<String> result = new ArrayList<>();
        if (header != null && !header.isEmpty()) {
            result.add(header);
        }
        for (String line : lines) {
            result.add(line);
        }
        return result;
    }

    static void fillBorder(ItemStack[] contents, Material glassMaterial) {
        int size = contents.length;
        if (size == 27) {
            for (int i = 0; i < 9; i++) {
                contents[i] = new ItemStack(glassMaterial);
                contents[18 + i] = new ItemStack(glassMaterial);
            }
            contents[9] = new ItemStack(glassMaterial);
            contents[17] = new ItemStack(glassMaterial);
        } else if (size == 54) {
            for (int i = 0; i < 9; i++) {
                contents[i] = new ItemStack(glassMaterial);
                contents[45 + i] = new ItemStack(glassMaterial);
            }
            for (int row = 1; row <= 4; row++) {
                contents[row * 9] = new ItemStack(glassMaterial);
                contents[row * 9 + 8] = new ItemStack(glassMaterial);
            }
        }
    }

    static void fillNavigationRow(ItemStack[] contents, int pageSize) {
        int rowStart = pageSize;
        for (int i = rowStart; i < rowStart + 9; i++) {
            if (contents[i] == null) {
                contents[i] = separator();
            }
        }
    }
}
