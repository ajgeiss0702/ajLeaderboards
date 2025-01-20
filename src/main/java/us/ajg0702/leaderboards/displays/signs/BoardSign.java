package us.ajg0702.leaderboards.displays.signs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.utils.spigot.LocUtils;

import java.util.concurrent.*;
import java.util.logging.Level;

public class BoardSign {
    private final Location location;
    private final String board;
    private final int position;
    private final TimedType type;

    private Sign sign;

    private final int x;
    private final int z;
    private final World world;

    private boolean removed;

    private final LeaderboardPlugin plugin;

    public BoardSign(Location location, String board, int position, TimedType type) {
        this.location = location;
        this.board = board;
        this.position = position;

        this.x = location.getBlockX() >> 4;
        this.z = location.getBlockZ() >> 4;
        this.world = location.getWorld();
        this.type = type;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("ajLeaderboards");
        if(plugin == null) throw new IllegalStateException("Where is ajleaderboards? I'm supposed to be ajLeaderboards, but im not?");
        this.plugin = (LeaderboardPlugin) plugin;
        setSign();
    }

    private Future<Sign> setSign() {
        CompletableFuture<Sign> future = new CompletableFuture<>();
        plugin.getScheduler().runSync(location, () -> {
            BlockState state = location.getBlock().getState();
            if(!(state instanceof Sign)) {
                sign = null;
            } else {
                sign = (Sign) state;
            }
            future.complete(sign);
        });
        return future;
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
        return sign;
    }

    public boolean isRemoved() {
        return removed;
    }
    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean isPlaced() throws ExecutionException, InterruptedException, TimeoutException {
        boolean placed = getBlockType(location).get(10, TimeUnit.SECONDS).contains("SIGN");
        if(placed && sign == null) {
            try {
                setSign().get();
            } catch (InterruptedException | ExecutionException e) {
                if(plugin.isShuttingDown()) return false;
                if(Debug.isDebug()) plugin.getLogger().log(Level.SEVERE, "Interrupted while trying to get sign data", e);
            }
        }
        return placed;
    }

    private Future<String> getBlockType(Location location) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if(plugin.isShuttingDown()) {
            future.completeExceptionally(new InterruptedException());
            return future;
        }
        plugin.getScheduler().runSync(location, () -> future.complete(location.getBlock().getType().toString()));
        return future;
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
