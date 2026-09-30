package com.soyadrianyt001.mypet.commands;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.gui.PetGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MyPetCommand implements CommandExecutor, TabCompleter {

    private final Mypet plugin;

    public MyPetCommand(Mypet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length >= 3) {
                    String mobType = args[1];
                    String petName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    plugin.getPetManager().createPet(player, mobType, petName);
                } else {
                    player.sendMessage(ChatColor.RED + "Uso: /mypet create <mob> <nombre>");
                    player.sendMessage(ChatColor.GRAY + "Ejemplo: /mypet create wolf Rocky");
                }
                break;

            case "gui":
            case "menu":
                PetGui.openMainMenu(player);
                break;

            case "creator":
                player.sendMessage(ChatColor.GOLD + "§l=== Mypet ===");
                player.sendMessage(ChatColor.YELLOW + "Creador: §fsoyadrianyt001");
                player.sendMessage(ChatColor.YELLOW + "Versión: §f1.0");
                player.sendMessage(ChatColor.YELLOW + "Plugin profesional de mascotas");
                player.sendMessage(ChatColor.GOLD + "=================");
                break;

            case "remove":
            case "delete":
                plugin.getPetManager().removePet(player);
                break;

            case "rename":
                if (args.length >= 2) {
                    String newName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    plugin.getPetManager().renamePet(player, newName);
                } else {
                    player.sendMessage(ChatColor.RED + "Uso: /mypet rename <nuevo_nombre>");
                }
                break;

            case "help":
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "§l=== Mypet - Comandos ===");
        player.sendMessage(ChatColor.YELLOW + "/mypet create <mob> <nombre> §7- Crear mascota");
        player.sendMessage(ChatColor.YELLOW + "/mypet gui §7- Abrir menú de gestión");
        player.sendMessage(ChatColor.YELLOW + "/mypet remove §7- Eliminar mascota");
        player.sendMessage(ChatColor.YELLOW + "/mypet rename <nombre> §7- Renombrar mascota");
        player.sendMessage(ChatColor.YELLOW + "/mypet creator §7- Información del plugin");
        player.sendMessage(ChatColor.GOLD + "=============================");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("create", "gui", "remove", "rename", "creator", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            for (EntityType type : EntityType.values()) {
                if (type.isSpawnable() && type.getEntityClass() != null &&
                    org.bukkit.entity.Mob.class.isAssignableFrom(type.getEntityClass())) {
                    completions.add(type.name().toLowerCase());
                }
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}
