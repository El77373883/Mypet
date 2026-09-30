package com.soyadrianyt001.mypet.managers;

import com.soyadrianyt001.mypet.Mypet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

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

        List<Component> messages = new ArrayList<>();
        for (String s : plugin.getConfig().getStringList("announcements.messages")) {
            messages.add(LEGACY.deserialize(s.replace('§', '&')));
        }
        if (messages.isEmpty()) {
            plugin.getLogger().warning("No hay mensajes de anuncio configurados.");
            return;
        }

        boolean soundEnabled = plugin.getConfig().getBoolean("announcements.sound.enabled", true);
        // Formato nuevo: "block.note_block.pling" (minúsculas con puntos)
        String soundKey = plugin.getConfig().getString("announcements.sound.sound-name", "block.note_block.pling");
        if (!soundKey.contains(".")) {
            plugin.getLogger().warning("Cambia announcements.sound.sound-name a formato 'block.note_block.pling'");
        }
        float volume = (float) plugin.getConfig().getDouble("announcements.sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("announcements.sound.pitch", 1.5);

        final int[] index = {0};
        announcementTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Component message = messages.get(index[0] % messages.size());
            index[0]++;
            plugin.getServer().broadcast(message);
            if (soundEnabled && soundKey.contains(".")) {
                plugin.getServer().getOnlinePlayers().forEach(p ->
                        p.playSound(p.getLocation(), soundKey, volume, pitch));
            }
        }, intervalTicks, intervalTicks);

        plugin.getLogger().info("Anuncios iniciados cada " + intervalSeconds + " segundos.");
    }

    public void stopAnnouncements() {
        if (announcementTask != null) announcementTask.cancel();
    }
}
