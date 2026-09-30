package com.soyadrianyt001.mypet.listeners;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.gui.PetGui;
import com.soyadrianyt001.mypet.managers.PetManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiListener implements Listener {

    private final Mypet plugin;
    /** jugador -> lo que se espera que escriba ("rename" o "create:<mob>"). */
    private final Map<UUID, String> pending = new ConcurrentHashMap<>();

    public GuiListener(Mypet plugin) {
        this.plugin = plugin;
    }

    private PetManager pm() {
        return plugin.getPetManager();
    }

    // ------------------------------------------------ menú: nada se puede sacar

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof PetGui.Holder holder)) return;
        e.setCancelled(true); // no se puede mover ni llevar nada del menú
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null
                || !e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        String action = holder.actions.get(e.getSlot());
        if (action == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> handle(p, action));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof PetGui.Holder) {
            e.setCancelled(true);
        }
    }

    private void handle(Player p, String action) {
        if (action.equals("close")) {
            p.closeInventory();
        } else if (action.startsWith("open:")) {
            try {
                PetGui.open(p, PetGui.Screen.valueOf(action.substring(5)));
            } catch (IllegalArgumentException ex) {
                p.closeInventory();
            }
        } else if (action.startsWith("cmd:")) {
            p.closeInventory();
            p.performCommand(action.substring(4));
        } else if (action.equals("prompt:rename")) {
            if (pm().getPet(p) == null) {
                p.closeInventory();
                pm().err(p, "No tienes una mascota.");
                return;
            }
            ask(p, "rename");
        } else if (action.startsWith("prompt:create:")) {
            if (pm().getPet(p) != null) {
                p.closeInventory();
                pm().err(p, "Ya tienes una mascota. Elimínala primero para crear otra.");
                return;
            }
            ask(p, "create:" + action.substring("prompt:create:".length()));
        }
    }

    private void ask(Player p, String what) {
        p.closeInventory();
        pending.put(p.getUniqueId(), what);
        pm().msg(p, "<yellow>✏ Escribe en el chat el <white>nombre <yellow>de tu mascota "
                + "<gray>(2-16 letras/números, o escribe <white>cancelar<gray>)");
    }

    // ------------------------------------------------ nombre por chat

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        String what = pending.get(p.getUniqueId());
        if (what == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        pending.remove(p.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (text.equalsIgnoreCase("cancelar")) {
                pm().err(p, "Cancelado.");
                return;
            }
            if (!text.matches("[\\p{L}\\p{N}_ ]{2,16}")) {
                pm().err(p, "Nombre inválido (2-16 letras, números, _ o espacios). Inténtalo otra vez.");
                pending.put(p.getUniqueId(), what);
                return;
            }
            if (what.equals("rename")) {
                pm().renamePet(p, text);
            } else if (what.startsWith("create:")) {
                pm().createPet(p, what.substring("create:".length()), text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }

    // ------------------------------------------------ protecciones extra

    /** Creeper / dragón: que no destruyan bloques ni hagan daño. */
    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }

    /** Zombies y esqueletos no se prenden con el sol. */
    @EventHandler
    public void onCombust(EntityCombustEvent e) {
        if (pm().isPet(e.getEntity())) e.setCancelled(true);
    }
}
