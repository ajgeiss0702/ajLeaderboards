package us.ajg0702.leaderboards.boards;

import org.bukkit.Bukkit;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.HashMap;

public class TopManager {


    private final LeaderboardPlugin plugin;
    public TopManager(LeaderboardPlugin pl) {
        plugin = pl;
    }

    private final HashMap<String, HashMap<TimedType, HashMap<Integer, Long>>> lastGet = new HashMap<>();
    private final HashMap<String, HashMap<TimedType, HashMap<Integer, StatEntry>>> cache = new HashMap<>();

    /**
     * Get a leaderboard position
     * @param position The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStat(int position, String board, TimedType type) {
        if(!cache.containsKey(board)) {
            cache.put(board, new HashMap<>());
        }
        if(!lastGet.containsKey(board)) {
            lastGet.put(board, new HashMap<>());
        }

        if(!cache.get(board).containsKey(type)) {
            cache.get(board).put(type, new HashMap<>());
        }
        if(!lastGet.get(board).containsKey(type)) {
            lastGet.get(board).put(type, new HashMap<>());
        }

        if(cache.get(board).get(type).containsKey(position)) {
            if(System.currentTimeMillis() - lastGet.get(board).get(type).get(position) > 5000) {
                lastGet.get(board).get(type).put(position, System.currentTimeMillis());
                fetchPositionAsync(position, board, type);
            }
            return cache.get(board).get(type).get(position);
        }

        lastGet.get(board).get(type).put(position, System.currentTimeMillis());
        return fetchPosition(position, board, type);
    }

    private void fetchPositionAsync(int position, String board, TimedType type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetchPosition(position, board, type));
    }
    private StatEntry fetchPosition(int position, String board, TimedType type) {
        StatEntry te = plugin.getCache().getStat(position, board, type);
        cache.get(board).get(type).put(position, te);
        return te;
    }

}

