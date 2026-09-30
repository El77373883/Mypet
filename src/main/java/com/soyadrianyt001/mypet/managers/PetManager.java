package com.soyadrianyt001.mypet.managers;

import com.soyadrianyt001.mypet.Mypet;
import com.soyadrianyt001.mypet.data.PetData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class PetManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N}_ ]{2,16}");

    private final Mypet plugin;
    private final File file;
    private final NamespacedKey ownerKey;
    /** owner -> mascota (todas las guardadas, estén online o no). */
    private final Map<UUID, PetData> pets = new HashMap<>();
    private boolean dirty;

    public PetManager(Mypet plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pets.yml");
        this.ownerKey = new NamespacedKey(plugin, "pet_owner");
        load();
    }

    // ------------------------------------------------------------ mensajes

    private Component legacy(String s) {
        return LEGACY.deserialize(s.replace('§', '&'));
    }

    private String cfg(String key, String def) {
        return plugin.getConfig().getString(key, def);
    }

    private Component prefix() {
        return legacy(cfg("messages.prefix", "&6[Mypet] &r"));
    }

    /** Mensaje con formato MiniMessage. */
    public void msg(Player p, String mini) {
        p.sendMessage(prefix().append(MM.deserialize(mini)));
    }

    /** Mensaje con colores clásicos (&) que viene del config.yml. */
    public void msgLegacy(Player p, String text) {
        p.sendMessage(prefix().append(legacy(text)));
    }

    public void ok(Player p, String text) {
        msg(p, "<green>" + MM.escapeTags(text));
    }

    public void err(Player p, String text) {
        msg(p, "<red>" + MM.escapeTags(text));
    }

    // ------------------------------------------------------------ consultas

    /** Todos los mobs del juego (zombies, esqueletos, golems, dragón...). */
    public List<String> allowedMobs() {
        List<String> out = new ArrayList<>();
        for (EntityType t : EntityType.values()) {
            if (t.getEntityClass() != null && Mob.class.isAssignableFrom(t.getEntityClass())) {
                out.add(t.name());
            }
        }
        return out;
    }

    public PetData getPet(Player player) {
        return pets.get(player.getUniqueId());
    }

    private PetData require(Player p) {
        PetData d = pets.get(p.getUniqueId());
        if (d == null) msgLegacy(p, cfg("messages.pet-not-found", "&cNo tienes una mascota."));
        return d;
    }

    public Mob getPetEntity(PetData d) {
        if (d.getPetUUID() == null) return null;
        Entity e = Bukkit.getEntity(d.getPetUUID());
        return (e instanceof Mob m && m.isValid()) ? m : null;
    }

    public boolean isPet(Entity e) {
        return e != null && e.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }

    public UUID getOwnerId(Entity e) {
        String s = e.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** ¿Esta entidad es la mascota actual de algún jugador? */
    public boolean isTracked(Entity e) {
        for (PetData d : pets.values()) {
            if (e.getUniqueId().equals(d.getPetUUID())) return true;
        }
        return false;
    }

    public Component displayName(PetData d) {
        return MM.deserialize("<dark_gray>[" + d.getRankColor() + d.getRankName() + "<dark_gray>] <gold>"
                + MM.escapeTags(d.getPetName()) + " <yellow>Nv." + d.getLevel());
    }

    public void refreshName(PetData d) {
        Mob m = getPetEntity(d);
        if (m != null) m.customName(displayName(d));
    }

    // -------------------------------------------------------------- acciones

    public void createPet(Player player, String mobType, String petName) {
        UUID uid = player.getUniqueId();
        if (pets.containsKey(uid)) {
            msgLegacy(player, cfg("messages.already-has-pet", "&cYa tienes una mascota."));
            return;
        }
        EntityType type;
        try {
            type = EntityType.valueOf(mobType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            msgLegacy(player, cfg("messages.invalid-mob", "&cEse mob no existe."));
            return;
        }
        if (!allowedMobs().contains(type.name())) {
            err(player, "Ese mob no está permitido. Abre /mypet gui para ver los disponibles.");
            return;
        }
        String name = petName.trim();
        if (!NAME.matcher(name).matches()) {
            err(player, "El nombre debe tener 2-16 caracteres (letras, números, _ o espacios).");
            return;
        }

        PetData data = new PetData(uid, null, type, name);
        pets.put(uid, data);
        Mob mob = spawn(player, data);
        if (mob == null) {
            pets.remove(uid);
            err(player, "No se pudo crear la mascota aquí.");
            return;
        }
        save();

        String m = cfg("messages.pet-created", "&aTu mascota %pet_name% fue creada.")
                .replace("%pet_name%", name);
        msgLegacy(player, m);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public void removePet(Player player) {
        PetData data = pets.remove(player.getUniqueId());
        if (data == null) {
            msgLegacy(player, cfg("messages.pet-not-found", "&cNo tienes una mascota."));
            return;
        }
        Mob m = getPetEntity(data);
        if (m != null) m.remove();
        save();
        msgLegacy(player, cfg("messages.pet-removed", "&aMascota eliminada."));
    }

    public void renamePet(Player player, String newName) {
        PetData data = require(player);
        if (data == null) return;
        String name = newName.trim();
        if (!NAME.matcher(name).matches()) {
            err(player, "El nombre debe tener 2-16 caracteres (letras, números, _ o espacios).");
            return;
        }
        data.setPetName(name);
        refreshName(data);
        save();
        msgLegacy(player, cfg("messages.pet-renamed", "&aNombre cambiado a %pet_name%.")
                .replace("%pet_name%", name));
    }

    public void toggleFollow(Player p) {
        PetData d = require(p);
        if (d == null) return;
        d.setFollowEnabled(!d.isFollowEnabled());
        save();
        ok(p, d.isFollowEnabled() ? "Tu mascota te seguirá." : "Tu mascota se quedará quieta.");
    }

    public void callPet(Player p) {
        PetData d = require(p);
        if (d == null) return;
        Mob m = getPetEntity(d);
        if (m == null) spawnPet(p);
        else m.teleport(p.getLocation());
        ok(p, "¡Tu mascota ya viene!");
    }

    public void setAttackMode(Player p, PetData.AttackMode mode) {
        PetData d = require(p);
        if (d == null) return;
        d.setAttackMode(mode);
        save();
        String label = switch (mode) {
            case ON -> "Atacar: ataca monstruos cercanos.";
            case DEFEND -> "Defender: solo ataca a quien te golpee.";
            case OFF -> "Pasivo: no ataca a nadie.";
        };
        ok(p, "Modo de combate → " + label);
    }

    public static String normalizeJob(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "none", "descanso", "ninguno" -> "none";
            case "collector", "recolector" -> "collector";
            case "farmer", "granjero" -> "farmer";
            case "miner", "minero" -> "miner";
            default -> null;
        };
    }

    public void setJob(Player p, String job) {
        PetData d = require(p);
        if (d == null) return;
        d.setCurrentJob(job);
        save();
        String label = switch (job) {
            case "collector" -> "Recolector: te trae los ítems del suelo.";
            case "farmer" -> "Granjero: cosecha y replanta cultivos.";
            case "miner" -> "Minero: pica minerales cercanos.";
            default -> "Descanso: solo te acompaña.";
        };
        ok(p, "Trabajo → " + label);
    }

    public void sendInfo(Player p) {
        PetData d = require(p);
        if (d == null) return;
        p.sendMessage(MM.deserialize("<gold><bold>=== " + MM.escapeTags(d.getPetName()) + " ==="));
        p.sendMessage(MM.deserialize("<yellow>Tipo: <white>" + d.getEntityType().name().toLowerCase(Locale.ROOT)));
        p.sendMessage(MM.deserialize("<yellow>Rango: " + d.getRankColor() + d.getRankName()
                + " <yellow>· Nivel <white>" + d.getLevel()));
        p.sendMessage(MM.deserialize(xpBar(d)));
        p.sendMessage(MM.deserialize("<yellow>Combate: <white>" + d.getAttackMode().name().toLowerCase(Locale.ROOT)
                + " <yellow>· Trabajo: <white>" + d.getCurrentJob()
                + " <yellow>· Seguir: " + (d.isFollowEnabled() ? "<green>Sí" : "<red>No")));
    }

    public String xpBar(PetData d) {
        int lvl = d.getLevel();
        if (lvl >= 50) return "<yellow>XP: <gold>MAX";
        long base = PetData.xpForLevel(lvl);
        long next = PetData.xpForLevel(lvl + 1);
        double prog = (double) (d.getXp() - base) / (double) (next - base);
        int filled = Math.max(0, Math.min(10, (int) Math.round(prog * 10)));
        return "<yellow>XP: <green>" + "█".repeat(filled) + "<dark_gray>" + "░".repeat(10 - filled)
                + " <gray>" + (d.getXp() - base) + "/" + (next - base);
    }

    public void addXp(Player owner, PetData d, int amount) {
        int before = d.getLevel();
        d.addXp(amount);
        dirty = true;
        if (d.getLevel() > before) {
            refreshName(d);
            Mob m = getPetEntity(d);
            if (m != null) {
                m.getWorld().spawnParticle(Particle.HEART, m.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4);
            }
            owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            msg(owner, "<white>" + MM.escapeTags(d.getPetName()) + " <gray>subió al <yellow>nivel " + d.getLevel()
                    + "<gray>. Rango: " + d.getRankColor() + d.getRankName());
            save();
        }
    }

    // ------------------------------------------------------- spawn / despawn

    /** Invoca la mascota del jugador (al entrar o si desapareció). */
    public Mob spawnPet(Player p) {
        PetData d = pets.get(p.getUniqueId());
        if (d == null) return null;
        Mob old = getPetEntity(d);
        if (old != null) old.remove();
        return spawn(p, d);
    }

    public void despawnPet(Player p) {
        PetData d = pets.get(p.getUniqueId());
        if (d == null) return;
        Mob m = getPetEntity(d);
        if (m != null) m.remove();
    }

    private Mob spawn(Player owner, PetData d) {
        Location loc = owner.getLocation();
        Entity e;
        try {
            e = loc.getWorld().spawnEntity(loc, d.getEntityType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
        } catch (Exception ex) {
            return null;
        }
        if (!(e instanceof Mob mob)) {
            if (e != null) e.remove();
            return null;
        }
        mob.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        mob.setPersistent(false);          // no se guarda en el mundo: cero mascotas huérfanas
        mob.setRemoveWhenFarAway(false);
        mob.setInvulnerable(true);
        mob.setCollidable(false);
        mob.setCanPickupItems(false);
        mob.setTarget(null);
        mob.setCustomNameVisible(true);
        mob.customName(displayName(d));
        if (mob instanceof Ageable a) {
            if (plugin.getConfig().getBoolean("pets.baby", true)) a.setBaby();
            a.setAgeLock(true);
        }
        mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4);
        d.setPetUUID(mob.getUniqueId());
        return mob;
    }

    // ------------------------------------------------------------ guardado

    public void markDirty() {
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    /** Se llama al apagar el plugin: quita las mascotas del mundo y guarda. */
    public void saveAllPets() {
        for (PetData d : pets.values()) {
            Mob m = getPetEntity(d);
            if (m != null) m.remove();
        }
        save();
        plugin.getLogger().info("Guardadas " + pets.size() + " mascotas.");
    }

    public void save() {
        dirty = false;
        YamlConfiguration yml = new YamlConfiguration();
        for (PetData d : pets.values()) {
            String b = "pets." + d.getOwnerUUID();
            yml.set(b + ".type", d.getEntityType().name());
            yml.set(b + ".name", d.getPetName());
            yml.set(b + ".xp", d.getXp());
            yml.set(b + ".follow", d.isFollowEnabled());
            yml.set(b + ".attack", d.getAttackMode().name());
            yml.set(b + ".job", d.getCurrentJob());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("No se pudo guardar pets.yml: " + ex.getMessage());
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("pets");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                ConfigurationSection s = sec.getConfigurationSection(key);
                EntityType type = EntityType.valueOf(s.getString("type", "COW"));
                PetData d = new PetData(owner, null, type, s.getString("name", "Mascota"));
                d.setXp(s.getLong("xp", 0));
                d.setFollowEnabled(s.getBoolean("follow", true));
                PetData.AttackMode am = PetData.AttackMode.parse(s.getString("attack", "ON"));
                d.setAttackMode(am == null ? PetData.AttackMode.ON : am);
                String job = normalizeJob(s.getString("job", "none"));
                d.setCurrentJob(job == null ? "none" : job);
                pets.put(owner, d);
            } catch (Exception ex) {
                plugin.getLogger().warning("Mascota ignorada (datos inválidos): " + key);
            }
        }
    }
}
