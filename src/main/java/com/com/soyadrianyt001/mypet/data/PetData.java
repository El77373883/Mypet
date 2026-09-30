package com.soyadrianyt001.mypet.data;

import org.bukkit.entity.EntityType;
import java.util.UUID;

public class PetData {

    private final UUID ownerUUID;
    private final UUID petUUID;
    private final EntityType entityType;
    private String petName;
    private boolean attackEnabled = true;
    private boolean followEnabled = true;
    private String currentJob = "none";

    public PetData(UUID ownerUUID, UUID petUUID, EntityType entityType, String petName) {
        this.ownerUUID = ownerUUID;
        this.petUUID = petUUID;
        this.entityType = entityType;
        this.petName = petName;
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public UUID getPetUUID() { return petUUID; }
    public EntityType getEntityType() { return entityType; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public boolean isAttackEnabled() { return attackEnabled; }
    public void setAttackEnabled(boolean attackEnabled) { this.attackEnabled = attackEnabled; }
    public boolean isFollowEnabled() { return followEnabled; }
    public void setFollowEnabled(boolean followEnabled) { this.followEnabled = followEnabled; }
    public String getCurrentJob() { return currentJob; }
    public void setCurrentJob(String currentJob) { this.currentJob = currentJob; }
}
