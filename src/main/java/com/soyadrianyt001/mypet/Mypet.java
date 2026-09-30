package com.soyadrianyt001.mypet;

import com.soyadrianyt001.mypet.commands.MyPetCommand;
import com.soyadrianyt001.mypet.listeners.PetListener;
import com.soyadrianyt001.mypet.managers.AnnouncementManager;
import com.soyadrianyt001.mypet.managers.PetManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mypet extends JavaPlugin {

    private static Mypet instance;
    private PetManager petManager;
    private AnnouncementManager announcementManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.petManager = new PetManager(this);
        this.announcementManager = new AnnouncementManager(this);

        getCommand("mypet").setExecutor(new MyPetCommand(this));
        getCommand("mypet").setTabCompleter(new MyPetCommand(this));

        getServer().getPluginManager().registerEvents(new PetListener(this), this);
        announcementManager.startAnnouncements();

        getLogger().info("§a[Mypet] Plugin activado - Versión 1.0 por soyadrianyt001");
    }

    @Override
    public void onDisable() {
        if (announcementManager != null) announcementManager.stopAnnouncements();
        if (petManager != null) petManager.saveAllPets();
        getLogger().info("§c[Mypet] Plugin desactivado");
    }

    public static Mypet getInstance() { return instance; }
    public PetManager getPetManager() { return petManager; }
    public AnnouncementManager getAnnouncementManager() { return announcementManager; }
}
