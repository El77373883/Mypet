package com.soyadrianyt001.mypet.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PetGui {

    public static void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "§l✦ Mypet - Panel de Control ✦");

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }

        gui.setItem(20, createItem(Material.EMERALD,
                ChatColor.GREEN + "§l✦ Crear Mascota ✦",
                ChatColor.GRAY + "Haz clic para crear una nueva mascota",
                ChatColor.GRAY + "Comando: /mypet create <mob> <nombre>"));

        gui.setItem(22, createItem(Material.BONE,
                ChatColor.YELLOW + "§l✦ Mi Mascota ✦",
                ChatColor.GRAY + "Haz clic para ver y gestionar tu mascota"));

        gui.setItem(24, createItem(Material.BARRIER,
                ChatColor.RED + "§l✦ Eliminar Mascota ✦",
                ChatColor.GRAY + "Haz clic para eliminar tu mascota"));

        gui.setItem(30, createItem(Material.NAME_TAG,
                ChatColor.AQUA + "§l✦ Renombrar Mascota ✦",
                ChatColor.GRAY + "Haz clic para cambiar el nombre"));

        gui.setItem(32, createItem(Material.DIAMOND_SWORD,
                ChatColor.RED + "§l✦ Modo Atacar ✦",
                ChatColor.GRAY + "Configura si tu mascota ataca"));

        gui.setItem(34, createItem(Material.IRON_PICKAXE,
                ChatColor.GOLD + "§l✦ Trabajos ✦",
                ChatColor.GRAY + "Asigna trabajos a tu mascota"));

        gui.setItem(49, createItem(Material.BARRIER,
                ChatColor.RED + "§l✦ Cerrar ✦",
                ChatColor.GRAY + "Haz clic para cerrar este menú"));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
