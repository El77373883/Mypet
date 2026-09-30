package com.soyadrianyt001.mypet.managers;

import com.soyadrianyt001.mypet.Mypet;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class AnnouncementManager {

    private final Mypet plugin;
    private BukkitTask announcementTask;

    public AnnouncementManager(Mypet plugin) {
        this.plugin = plugin;
    }

    public void startAnnouncements() {
        if (!plugin.getConfig().getBoolean("announcements.enabled", true)) {
            plugin.getLogger().info("Anuncios automáticos desactivados.");
            return;
        }

        int intervalSeconds = plugin.getConfig().getInt("announcements.interval-seconds", 300);
        long intervalTicks = intervalSeconds * 20L;

        List<String> messages = plugin.getConfig().getStringList("announcements.messages");
        if (messages.isEmpty()) {
            plugin.getLogger().warning("No hay mensajes de anuncio configurados.");
            return;
        }

        boolean soundEnabled = plugin.getConfig().getBoolean("announcements.sound.enabled", true);
        String soundName = plugin.getConfig().getString("announcements.sound.sound-name", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) plugin.getConfig().getDouble("announcements.sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("announcements.sound.pitch", 1.5);

        final int[] index = {0};

        announcementTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            String message = messages.get(index[0] % messages.size());
            index[0]++;

            plugin.getServer().broadcastMessage(message);

            if (soundEnabled) {
                try {
                    Sound sound = Sound.valueOf(soundName);
                    plugin.getServer().getOnlinePlayers().forEach(p ->
                            p.playSound(p.getLocation(), sound, volume, pitch));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Sonido inválido: " + soundName);
                }
            }
        }, intervalTicks, intervalTicks);

        plugin.getLogger().info("Anuncios iniciados cada " + intervalSeconds + " segundos.");
    }

    public void stopAnnouncements() {
        if (announcementTask != null) announcementTask.cancel();
    }
}
