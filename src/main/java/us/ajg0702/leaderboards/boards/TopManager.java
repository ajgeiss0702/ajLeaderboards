package us.ajg0702.leaderboards.boards;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TopManager {

    private final ThreadPoolExecutor fetchService = new ThreadPoolExecutor(
            0, 50,
            5L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500, true)
    );

    

    public void shutdown() {
        fetchService.shutdownNow();
    }


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
        if(plugin.getAConfig().getBoolean("blocking-fetch")) {
            return fetchPosition(position, board, type);
        } else {
            fetchPositionAsync(position, board, type);
            return new StatEntry(plugin, -2, board, "", "Loading", null, "", 0, type);
        }
    }
    AtomicInteger fetching = new AtomicInteger(0);
    private void fetchPositionAsync(int position, String board, TimedType type) {
        if(plugin.isShuttingDown()) return;
        checkWrong();
        fetchService.submit(() -> fetchPosition(position, board, type));
    }
    private StatEntry fetchPosition(int position, String board, TimedType type) {
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching ("+fetchService.getPoolSize()+") (pos): "+fetching.getAndIncrement());
        StatEntry te = plugin.getCache().getStat(position, board, type);
        cache.get(board).get(type).put(position, te);
        fetching.decrementAndGet();
        return te;
    }


    private final HashMap<String, HashMap<TimedType, HashMap<OfflinePlayer, Long>>> lastGetSE = new HashMap<>();
    private final HashMap<String, HashMap<TimedType, HashMap<OfflinePlayer, StatEntry>>> cacheSE = new HashMap<>();

    /**
     * Get a leaderboard position
     * @param player The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
        if(!cacheSE.containsKey(board)) {
            cacheSE.put(board, new HashMap<>());
        }
        if(!lastGetSE.containsKey(board)) {
            lastGetSE.put(board, new HashMap<>());
        }

        if(!cacheSE.get(board).containsKey(type)) {
            cacheSE.get(board).put(type, new HashMap<>());
        }
        if(!lastGetSE.get(board).containsKey(type)) {
            lastGetSE.get(board).put(type, new HashMap<>());
        }

        if(cacheSE.get(board).get(type).containsKey(player)) {
            if(System.currentTimeMillis() - lastGetSE.get(board).get(type).get(player) > 5000) {
                lastGetSE.get(board).get(type).put(player, System.currentTimeMillis());
                fetchStatEntryAsync(player, board, type);
            }
            return cacheSE.get(board).get(type).get(player);
        }

        lastGetSE.get(board).get(type).put(player, System.currentTimeMillis());
        if(plugin.getAConfig().getBoolean("blocking-fetch")) {
            return fetchStatEntry(player, board, type);
        } else {
            fetchStatEntryAsync(player, board, type);
            return new StatEntry(plugin, -2, board, "", player.getName(), player.getUniqueId(), "", 0, type);
        }
    }

    private void fetchStatEntryAsync(OfflinePlayer player, String board, TimedType type) {
        checkWrong();
        fetchService.submit(() -> fetchStatEntry(player, board, type));
    }
    private StatEntry fetchStatEntry(OfflinePlayer player, String board, TimedType type) {
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching ("+fetchService.getPoolSize()+") (statentry): "+fetching.getAndIncrement());
        StatEntry te = plugin.getCache().getStatEntry(player, board, type);
        cacheSE.get(board).get(type).put(player, te);
        fetching.decrementAndGet();
        return te;
    }

    List<String> boardCache;
    long lastGetBoard = 0;
    public List<String> getBoards() {
        if(boardCache == null) {
            if(plugin.getAConfig().getBoolean("blocking-fetch")) {
                return fetchBoards();
            } else {
                fetchBoardsAsync();
                return new ArrayList<>();
            }
        }

        if(System.currentTimeMillis() - lastGetBoard > 1000) {
            lastGetBoard = System.currentTimeMillis();
            fetchBoardsAsync();
        }
        return boardCache;
    }

    private void fetchBoardsAsync() {
        checkWrong();
        fetchService.submit(this::fetchBoards);
    }
    private List<String> fetchBoards() {
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching ("+fetchService.getPoolSize()+") (boards): "+fetching.getAndIncrement());
        boardCache = plugin.getCache().getBoards();
        fetching.decrementAndGet();
        return boardCache;
    }



    private void checkWrong() {
        if(fetching.get() > 150) {
            plugin.getLogger().warning("Something might be going wrong, printing some useful info");
            Thread.dumpStack();
        }
    }

}

