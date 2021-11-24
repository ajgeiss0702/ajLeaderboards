package us.ajg0702.leaderboards.boards;

import org.bukkit.Bukkit;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.HashMap;

public class TopManager {


    private final LeaderboardPlugin plugin;
    public TopManager(LeaderboardPlugin pl) {
        plugin = pl;
    }

    private final HashMap<String, HashMap<Integer, Long>> lastGet = new HashMap<>();
    private final HashMap<String, HashMap<Integer, StatEntry>> cache = new HashMap<>();

    /**
     * Get a leaderboard position
     * @param position The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStat(int position, String board) {
        if(!cache.containsKey(board)) {
            cache.put(board, new HashMap<>());
        }
        if(!lastGet.containsKey(board)) {
            lastGet.put(board, new HashMap<>());
        }

        if(cache.get(board).containsKey(position)) {
            if(System.currentTimeMillis() - lastGet.get(board).get(position) > 5000) {
                lastGet.get(board).put(position, System.currentTimeMillis());
                fetchPositionAsync(position, board);
            }
            return cache.get(board).get(position);
        }

        lastGet.get(board).put(position, System.currentTimeMillis());
        return fetchPosition(position, board);
    }

    private void fetchPositionAsync(int position, String board) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetchPosition(position, board));
    }
    private StatEntry fetchPosition(int position, String board) {
        StatEntry te = plugin.getCache().getStat(position, board);
        cache.get(board).put(position, te);
        return te;
    }

}

