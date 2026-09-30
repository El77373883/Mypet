package com.soyadrianyt001.mypet.commands;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.data.PetData;
import com.soyadrianyt001.mypet.gui.PetGui;
import com.soyadrianyt001.mypet.managers.PetManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MyPetCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final List<String> SUBS = List.of(
            "create", "gui", "remove", "rename", "follow", "call", "attack", "job", "info", "creator", "help");

    private final Mypet plugin;

    public MyPetCommand(Mypet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }
        PetManager pm = plugin.getPetManager();
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create", "crear" -> {
                if (args.length >= 3) {
                    String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    pm.createPet(player, args[1], name);
                } else {
                    pm.err(player, "Uso: /mypet create <mob> <nombre>  (ej: /mypet create wolf Rocky)");
                }
            }
            case "gui", "menu" -> PetGui.openMainMenu(player);
            case "remove", "delete", "eliminar" -> pm.removePet(player);
            case "rename", "nombre" -> {
                if (args.length >= 2) {
                    pm.renamePet(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                } else {
                    pm.err(player, "Uso: /mypet rename <nuevo_nombre>");
                }
            }
            case "follow", "seguir" -> pm.toggleFollow(player);
            case "call", "llamar" -> pm.callPet(player);
            case "attack", "atacar" -> {
                PetData.AttackMode m = args.length > 1 ? PetData.AttackMode.parse(args[1]) : null;
                if (m == null) pm.err(player, "Uso: /mypet attack <on|defend|off>");
                else pm.setAttackMode(player, m);
            }
            case "job", "trabajo" -> {
                String job = args.length > 1 ? PetManager.normalizeJob(args[1]) : null;
                if (job == null) pm.err(player, "Uso: /mypet job <none|collector|farmer|miner>");
                else pm.setJob(player, job);
            }
            case "info" -> pm.sendInfo(player);
            case "creator" -> playCreatorShow(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void playCreatorShow(Player p) {
        // Mensaje en el chat (con links clickeables)
        p.sendMessage(MM.deserialize("<gradient:gold:red><bold>━━━━━━━━━━━━━━━━━━━━━━"));
        p.sendMessage(MM.deserialize("<yellow>👑 Creador: <rainbow><bold>soyadrianyt001"));
        p.sendMessage(MM.deserialize("<aqua>🔥 ¡Sígueme en todas las redes!"));
        p.sendMessage(MM.deserialize(
                "<click:open_url:'https://youtube.com/@TU_USUARIO'><red>▶ YouTube</click>  "
                        + "<click:open_url:'https://tiktok.com/@TU_USUARIO'><light_purple>♪ TikTok</click>  "
                        + "<click:open_url:'https://instagram.com/TU_USUARIO'><gold>📷 Instagram</click>"));
        p.sendMessage(MM.deserialize("<gradient:red:gold><bold>━━━━━━━━━━━━━━━━━━━━━━"));

        // Animación de título + actionbar con colores arcoíris que se mueven
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!p.isOnline() || tick >= 60) {
                    if (p.isOnline()) {
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    }
                    cancel();
                    return;
                }
                String rb = "<rainbow:" + tick + ">";
                Component title = MM.deserialize(rb + "<bold>SOYADRIANYT001");
                Component sub = MM.deserialize(rb + "¡Sígueme en todas las redes!");
                p.showTitle(Title.title(title, sub,
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ZERO)));
                p.sendActionBar(MM.deserialize(rb + "★ Mypet by soyadrianyt001 ★"));

                if (tick % 4 == 0) {
                    float pitch = 0.5f + (tick % 24) / 24f * 1.5f;
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, pitch);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void sendHelp(Player p) {
        p.sendMessage(MM.deserialize("<gold><bold>=== Mypet · Comandos ==="));
        String[] lines = {
                "/mypet create <mob> <nombre> <gray>- crear mascota",
                "/mypet gui <gray>- abrir el menú",
                "/mypet rename <nombre> <gray>- renombrar",
                "/mypet remove <gray>- eliminar",
                "/mypet follow <gray>- seguir / quedarse",
                "/mypet call <gray>- llamar a tu mascota",
                "/mypet attack <on|defend|off> <gray>- modo de combate",
                "/mypet job <none|collector|farmer|miner> <gray>- trabajo",
                "/mypet info <gray>- nivel y estado",
                "/mypet creator <gray>- info del creador"
        };
        for (String l : lines) p.sendMessage(MM.deserialize("<yellow>" + l));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBS);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create", "crear" -> {
                    for (String m : plugin.getPetManager().allowedMobs()) options.add(m.toLowerCase(Locale.ROOT));
                }
                case "attack", "atacar" -> options.addAll(List.of("on", "defend", "off"));
                case "job", "trabajo" -> options.addAll(List.of("none", "collector", "farmer", "miner"));
                default -> {
                }
            }
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.startsWith(typed)).toList();
    }
}
