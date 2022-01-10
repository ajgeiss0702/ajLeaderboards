package us.ajg0702.leaderboards.displays.armorstands;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.UUID;

@SuppressWarnings("unused")
public class ArmorStandCache {

    private final Location location;
    private final Entity entity;
    private final UUID id;

    public ArmorStandCache(Location location, Entity entity, UUID id) {
        this.location = location;
        this.entity = entity;
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public Entity getEntity() {
        return entity;
    }

    public UUID getId() {
        return id;
    }
}
