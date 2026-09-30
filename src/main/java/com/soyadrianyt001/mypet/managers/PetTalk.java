package com.soyadrianyt001.mypet.managers;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.data.PetData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Hace que la mascota hable: "tengo hambre", "tengo frío", etc. */
public class PetTalk {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Map<String, List<String>> DEFAULTS = Map.of(
            "lowhealth", List.of(
                    "¡Cuidado, tienes poca vida!",
                    "¡Estás herido! Cúrate, por favor.",
                    "No te mueras, %owner%..."),
            "ownerhungry", List.of(
                    "¡Tienes hambre! Come algo.",
                    "Tu estómago está sonando... ¡come!",
                    "Yo también tengo hambre, ¡comamos!"),
            "cold", List.of(
                    "¡Tengo frío! Brrr...",
                    "Qué frío hace aquí...",
                    "¿Podemos ir a un lugar más calentito?"),
            "hot", List.of(
                    "¡Tengo calor!",
                    "Uf, qué calor... necesito agua.",
                    "Aquí hace demasiado calor."),
            "rain", List.of(
                    "¡Me estoy mojando!",
                    "Está lloviendo, ¡busquemos refugio!",
                    "No me gusta la lluvia..."),
            "night", List.of(
                    "Tengo miedo de la oscuridad...",
                    "Ya es de noche, tengo sueño.",
                    "¡Cuidado con los monstruos!"),
            "idle", List.of(
                    "¡Tengo hambre!",
                    "Tengo sed...",
                    "Tengo sueño.",
                    "¡Estoy aburrido, juguemos!",
                    "¡Te quiero, %owner%!",
                    "¿Vamos a explorar?",
                    "Me duelen las patitas de tanto caminar."));

    private final Mypet plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> next = new HashMap<>();

    public PetTalk(Mypet plugin) {
        this.plugin = plugin;
    }

    public void forget(UUID owner) {
        next.remove(owner);
    }

    /** Se llama en cada tick de la mascota; solo habla cuando toca. */
    public void tick(PetManager pm, Player owner, PetData d, Mob pet) {
        if (!plugin.getConfig().getBoolean("pets.talk.enabled", true)) return;
        long now = System.currentTimeMillis();
        UUID id = owner.getUniqueId();
        Long due = next.get(id);
        if (due == null) {
            schedule(id, now);
            return;
        }
        if (now < due) return;
        schedule(id, now);
        say(pm, owner, d, pet, message(pick(owner, pet), owner));
    }

    private void schedule(UUID id, long now) {
        int base = Math.max(5, plugin.getConfig().getInt("pets.talk.interval-seconds", 90));
        long delay = (base + random.nextInt(Math.max(1, base / 2))) * 1000L;
        next.put(id, now + delay);
    }

    private String pick(Player owner, Mob pet) {
        Location l = pet.getLocation();
        World w = l.getWorld();
        String category = "idle";

        if (owner.getHealth() <= 6.0) {
            category = "lowhealth";
        } else if (owner.getFoodLevel() <= 6) {
            category = "ownerhungry";
        } else {
            double temp = w.getTemperature(l.getBlockX(), l.getBlockY(), l.getBlockZ());
            long time = w.getTime();
            if (temp <= 0.15) category = "cold";
            else if (temp >= 1.5) category = "hot";
            else if (w.hasStorm() && w.getHighestBlockYAt(l) <= l.getBlockY()) category = "rain";
            else if (w.getEnvironment() == World.Environment.NORMAL && time >= 13000 && time <= 23000) category = "night";
        }
        // para que no repita siempre lo mismo, a veces dice algo cualquiera
        if (!category.equals("idle") && !category.equals("lowhealth") && random.nextInt(100) >= 65) {
            category = "idle";
        }
        return category;
    }

    private String message(String category, Player owner) {
        List<String> list = plugin.getConfig().getStringList("pets.talk.messages." + category);
        if (list.isEmpty()) list = DEFAULTS.get(category);
        return list.get(random.nextInt(list.size())).replace("%owner%", owner.getName());
    }

    private void say(PetManager pm, Player owner, PetData d, Mob pet, String text) {
        String name = MM.escapeTags(d.getPetName());
        String msg = MM.escapeTags(text);

        // burbuja: el nombre sobre la mascota cambia unos segundos
        pet.customName(MM.deserialize("<gold>" + name + "<gray>: <white>" + msg));
        Bukkit.getScheduler().runTaskLater(plugin, () -> pm.refreshName(d), 80L);

        if (plugin.getConfig().getBoolean("pets.talk.chat", true)) {
            owner.sendMessage(MM.deserialize("<gold><bold>" + name + "<reset><gray>: <white>" + msg));
        }
        pet.getWorld().spawnParticle(Particle.NOTE, pet.getLocation().add(0, 1.2, 0), 3, 0.3, 0.3, 0.3);
        owner.playSound(pet.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.6f);
    }
}
