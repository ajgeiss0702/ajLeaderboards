package us.ajg0702.leaderboards.boards;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class TopManager {

    /*private final ThreadPoolExecutor fetchService = new ThreadPoolExecutor(
            0, 10,
            5L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500, true)
    );*/
    private final ThreadPoolExecutor fetchService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    

    public void shutdown() {
        fetchService.shutdownNow();
    }


    private final LeaderboardPlugin plugin;
    public TopManager(LeaderboardPlugin pl) {
        plugin = pl;
        fetchService.setThreadFactory(new DefaultThreadFactory("AJLBFETCH"));
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            rolling.add(fetching.get());
            if(rolling.size() > 50) {
                rolling.remove(0);
            }
        }, 0, 2);
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
            if(System.currentTimeMillis() - lastGet.get(board).get(type).get(position) > cacheTime()) {
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
            return StatEntry.loading(plugin, board, type);
        }
    }
    AtomicInteger fetching = new AtomicInteger(0);
    private void fetchPositionAsync(int position, String board, TimedType type) {
        if(plugin.isShuttingDown()) return;
        checkWrong();
        fetchService.submit(() -> fetchPosition(position, board, type));
    }
    private StatEntry fetchPosition(int position, String board, TimedType type) {
        int f = fetching.getAndIncrement();
        if(!cache.get(board).get(type).containsKey(position)) {
            cache.get(board).get(type).put(position, StatEntry.loading(plugin, board, type));
        }
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching ("+fetchService.getPoolSize()+") (pos): "+f);
        StatEntry te = plugin.getCache().getStat(position, board, type);
        cache.get(board).get(type).put(position, te);
        removeFetching();
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
            if(System.currentTimeMillis() - lastGetSE.get(board).get(type).get(player) > cacheTime()) {
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
        int f = fetching.getAndIncrement();
        if(!cacheSE.get(board).get(type).containsKey(player)) {
            cacheSE.get(board).get(type).put(player, StatEntry.loading(plugin, board, type));
        }
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching ("+fetchService.getPoolSize()+") (statentry): "+f);
        StatEntry te = plugin.getCache().getStatEntry(player, board, type);
        cacheSE.get(board).get(type).put(player, te);
        removeFetching();
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

        if(System.currentTimeMillis() - lastGetBoard > cacheTime()) {
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
        int f = fetching.getAndIncrement();
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching ("+fetchService.getPoolSize()+") (boards): "+f);
        boardCache = plugin.getCache().getBoards();
        removeFetching();
        return boardCache;
    }

    List<Integer> rolling = new ArrayList<>();
    private void removeFetching() {
        fetching.decrementAndGet();
    }

    public int getFetching() {
        return fetching.get();
    }

    public int getFetchingAverage() {
        List<Integer> snap = new ArrayList<>(rolling);
        int sum = 0;
        for(Integer n : snap) {
            if(n == null) break;
            sum += n;
        }
        return sum/snap.size();
    }

    public int getActiveFetchers() {
        return fetchService.getActiveCount();
    }

    private void checkWrong() {
        if(fetching.get() > 5000) {
            plugin.getLogger().warning("Something might be going wrong, printing some useful info");
            Thread.dumpStack();
        }
    }

    public int cacheTime() {

        int r = 2000;

        int fetchingAverage = getFetchingAverage();

        if(fetchingAverage == Integer.MAX_VALUE) {
            return r;
        }

        if(fetchingAverage >= 1) {
            r = 5000;
        }
        if(fetchingAverage >= 5) {
            r = 10000;
        }
        if(fetchingAverage > 10) {
            r = 15000;
        }
        if(fetchingAverage > 20) {
            r = 30000;
        }
        if(fetchingAverage > 30) {
            r = 60000;
        }
        if(fetchingAverage > 50) {
            r = 120000;
        }
        if(fetchingAverage > 100) {
            r = 180000;
        }
        if(fetchingAverage > 1000) {
            r = 3600000;
        }


        return r;
    }

}

