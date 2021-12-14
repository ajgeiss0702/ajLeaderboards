package us.ajg0702.leaderboards.displays.signs;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.utils.spigot.LocUtils;

public class BoardSign {
    private final Location location;
    private final String board;
    private final int position;
    private final TimedType type;

    private final int x;
    private final int z;
    private final World world;

    public BoardSign(Location location, String board, int position, TimedType type) {
        this.location = location;
        this.board = board;
        this.position = position;

        this.x = location.getChunk().getX();
        this.z = location.getChunk().getZ();
        this.world = location.getWorld();
        this.type = type;
    }

    public int getX() {
        return x;
    }
    public int getZ() {
        return z;
    }
    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return location;
    }
    public String getBoard() {
        return board;
    }
    public int getPosition() {
        return position;
    }
    public TimedType getType() {
        return type;
    }

    public Sign getSign() {
        BlockState state = location.getBlock().getState();
        if(!(state instanceof Sign)) return null;

        return (Sign) state;
    }

    public void setText(String line1, String line2, String line3, String line4) {
        BlockState state = location.getBlock().getState();
        if(!(state instanceof Sign)) return;

        Sign sign = (Sign) state;
        sign.setLine(0, line1);
        sign.setLine(1, line2);
        sign.setLine(2, line3);
        sign.setLine(3, line4);
        sign.update();
    }

    public String serialize() {
        return LocUtils.locToString(location)+";"+board+";"+position+";"+type;
    }

    public static BoardSign deserialize(String s) {
        String[] parts = s.split(";");
        Location loc = LocUtils.stringToLoc(parts[0]);
        String board = parts[1];
        int pos = Integer.parseInt(parts[2]);
        TimedType type = parts.length > 3 ? TimedType.valueOf(parts[3]) : TimedType.ALLTIME;
        return new BoardSign(loc, board, pos, type);
    }
}
