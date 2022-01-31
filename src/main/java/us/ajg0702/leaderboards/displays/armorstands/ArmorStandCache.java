package us.ajg0702.leaderboards.displays.armorstands;

import org.bukkit.entity.ArmorStand;

import java.util.UUID;

@SuppressWarnings("unused")
public class ArmorStandCache {
    private final ArmorStand entity;
    private UUID id;

    public ArmorStandCache(ArmorStand entity, UUID id) {
        this.entity = entity;
        this.id = id;
    }

    public ArmorStand getEntity() {
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
