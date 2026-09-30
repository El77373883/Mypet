package com.soyadrianyt001.mypet.managers;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.data.PetData;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetManager {

    private final Mypet plugin;
    private final Map<UUID, PetData> activePets = new HashMap<>();

    public PetManager(Mypet plugin) {
        this.plugin = plugin;
    }

    public void createPet(Player player, String mobType, String petName) {
        if (activePets.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.already-has-pet"));
            return;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(mobType.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.invalid-mob"));
            return;
        }

        if (type.getEntityClass() == null || !Mob.class.isAssignableFrom(type.getEntityClass())) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") +
                    "§cEse tipo de entidad no es un mob válido.");
            return;
        }

        Mob pet = (Mob) player.getWorld().spawnEntity(player.getLocation(), type);
        pet.setCustomName(ChatColor.GOLD + petName);
        pet.setCustomNameVisible(true);
        pet.setRemoveWhenFarAway(false);

        PetData data = new PetData(player.getUniqueId(), pet.getUniqueId(), type, petName);
        activePets.put(player.getUniqueId(), data);

        String msg = plugin.getConfig().getString("messages.pet-created")
                .replace("%pet_name%", petName);
        player.sendMessage(plugin.getConfig().getString("messages.prefix") + msg);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public void removePet(Player player) {
        PetData data = activePets.remove(player.getUniqueId());
        if (data == null) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.pet-not-found"));
            return;
        }

        var entity = plugin.getServer().getEntity(data.getPetUUID());
        if (entity != null) entity.remove();

        player.sendMessage(plugin.getConfig().getString("messages.prefix") +
                plugin.getConfig().getString("messages.pet-removed"));
    }

    public void renamePet(Player player, String newName) {
        PetData data = activePets.get(player.getUniqueId());
        if (data == null) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.pet-not-found"));
            return;
        }

        data.setPetName(newName);
        var entity = plugin.getServer().getEntity(data.getPetUUID());
        if (entity != null) entity.setCustomName(ChatColor.GOLD + newName);

        String msg = plugin.getConfig().getString("messages.pet-renamed")
                .replace("%pet_name%", newName);
        player.sendMessage(plugin.getConfig().getString("messages.prefix") + msg);
    }

    public PetData getPet(Player player) {
        return activePets.get(player.getUniqueId());
    }

    public void saveAllPets() {
        plugin.getLogger().info("Guardando " + activePets.size() + " mascotas...");
    }
}
