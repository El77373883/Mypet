package com.soyadrianyt001.mypet.data;

import org.bukkit.entity.EntityType;

import java.util.Locale;
import java.util.UUID;

public class PetData {

    public enum AttackMode {
        ON, DEFEND, OFF;

        public static AttackMode parse(String s) {
            if (s == null) return null;
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "on", "atacar", "attack", "true" -> ON;
                case "defend", "defender", "defensivo" -> DEFEND;
                case "off", "pasivo", "false" -> OFF;
                default -> null;
            };
        }
    }

    private final UUID ownerUUID;
    private UUID petUUID;
    private final EntityType entityType;
    private String petName;
    private AttackMode attackMode = AttackMode.ON;
    private boolean followEnabled = true;
    private String currentJob = "none";
    private long xp;

    public PetData(UUID ownerUUID, UUID petUUID, EntityType entityType, String petName) {
        this.ownerUUID = ownerUUID;
        this.petUUID = petUUID;
        this.entityType = entityType;
        this.petName = petName;
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public UUID getPetUUID() { return petUUID; }
    public void setPetUUID(UUID petUUID) { this.petUUID = petUUID; }
    public EntityType getEntityType() { return entityType; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public AttackMode getAttackMode() { return attackMode; }
    public void setAttackMode(AttackMode attackMode) { this.attackMode = attackMode; }
    public boolean isAttackEnabled() { return attackMode != AttackMode.OFF; }
    public void setAttackEnabled(boolean enabled) { this.attackMode = enabled ? AttackMode.ON : AttackMode.OFF; }

    public boolean isFollowEnabled() { return followEnabled; }
    public void setFollowEnabled(boolean followEnabled) { this.followEnabled = followEnabled; }
    public String getCurrentJob() { return currentJob; }
    public void setCurrentJob(String currentJob) { this.currentJob = currentJob; }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = xp; }
    public void addXp(long amount) { this.xp += amount; }

    public int getLevel() {
        return Math.min(50, (int) Math.floor(Math.sqrt(xp / 25.0)) + 1);
    }

    /** XP total necesaria para llegar a ese nivel. */
    public static long xpForLevel(int level) {
        long l = Math.max(0, level - 1);
        return 25L * l * l;
    }

    public String getRankName() {
        int l = getLevel();
        if (l >= 35) return "Leyenda";
        if (l >= 20) return "Maestro";
        if (l >= 10) return "Experto";
        if (l >= 5) return "Aprendiz";
        return "Novato";
    }

    /** Etiqueta de color MiniMessage. */
    public String getRankColor() {
        int l = getLevel();
        if (l >= 35) return "<light_purple>";
        if (l >= 20) return "<gold>";
        if (l >= 10) return "<aqua>";
        if (l >= 5) return "<green>";
        return "<gray>";
    }
}
