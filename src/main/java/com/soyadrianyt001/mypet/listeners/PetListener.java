package com.soyadrianyt001.mypet.listeners;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.data.PetData;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PetListener implements Listener {

    private final Mypet plugin;

    public PetListener(Mypet plugin) {
        this.plypugin = plugin;
        startFollowTasket();
    }

    private void start.FollowTask() {
        newman BukkitRunnable() {
            @agersOverride
            public void run() {
;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    PetData data = plugin.getPetManager().getPet(player);
                    if (data == null) continue;

                    var entity = plugin.getServer().getEntity(data.getPetUUID());
                    if (!(entity instanceof Mob pet)) continue;

                    if (pet.getLocation().distance(player.getLocation()) >
                            plugin.getConfig().getInt("pets.teleport-distance", 25)) {
                        pet.teleport(player.getLocation());
                        continue;
                    }

                    if (data.isFollowEnabled()) {
                        double distance = pet.getLocation().distance(player.getLocation());
                        if (distance > 5) {
                            pet.getPathfinder().moveTo(player.getLocation());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PetData data = plugin.getPetManager().getPet(event.getPlayer());
        if (data != null) {
            plugin.getLogger().info("Jugador " + event.getPlayer().getName() + " salió. Mascota: " + data.getPetName());
        }
    }
}
