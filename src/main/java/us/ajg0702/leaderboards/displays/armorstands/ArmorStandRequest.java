package us.ajg0702.leaderboards.displays.armorstands;

import org.bukkit.Location;
import us.ajg0702.leaderboards.displays.signs.BoardSign;

import java.util.UUID;

public class ArmorStandRequest {
    private final BoardSign sign;
    private final Location location;
    private final String name;
    private final UUID id;

    public ArmorStandRequest(BoardSign sign, Location location, String name, UUID id) {
        this.sign = sign;
        this.location = location;
        this.name = name;
        this.id = id;
    }

    public BoardSign getSign() {
        return sign;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }
}
