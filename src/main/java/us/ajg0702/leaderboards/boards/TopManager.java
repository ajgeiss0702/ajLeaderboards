package us.ajg0702.leaderboards.boards;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.leaderboards.cache.methods.SqliteMethod;
import us.ajg0702.leaderboards.utils.Cached;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TopManager {

    private final ThreadPoolExecutor fetchService;
    //private final ThreadPoolExecutor fetchService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    

    public void shutdown() {
        fetchService.shutdownNow();
    }


    private final LeaderboardPlugin plugin;
    public TopManager(LeaderboardPlugin pl) {
        plugin = pl;
        CacheMethod method = plugin.getCache().getMethod();
        int t = method instanceof SqliteMethod ? 50 : Math.max(10, method.getMaxConnections());
        fetchService = new ThreadPoolExecutor(
                t, t,
                30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000000, true)
        );
        fetchService.allowCoreThreadTimeOut(true);
        fetchService.setThreadFactory(new DefaultThreadFactory("AJLBFETCH"));
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            rolling.add(getQueuedTasks()+getActiveFetchers());
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
        if(!cache.get(board).get(type).containsKey(position)) {
            cache.get(board).get(type).put(position, StatEntry.loading(plugin, board, type));
        }
        checkWrong();
        fetchService.submit(() -> fetchPosition(position, board, type));
    }
    private StatEntry fetchPosition(int position, String board, TimedType type) {
        int f = fetching.getAndIncrement();
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
            return new StatEntry(plugin, -2, board, "", player.getName(), player.getName(), player.getUniqueId(), "", 0, type);
        }
    }

    private void fetchStatEntryAsync(OfflinePlayer player, String board, TimedType type) {
        if(!cacheSE.get(board).get(type).containsKey(player)) {
            cacheSE.get(board).get(type).put(player, StatEntry.loading(plugin, board, type));
        }
        checkWrong();
        fetchService.submit(() -> fetchStatEntry(player, board, type));
    }
    private StatEntry fetchStatEntry(OfflinePlayer player, String board, TimedType type) {
        int f = fetching.getAndIncrement();
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

    public void fetchBoardsAsync() {
        checkWrong();
        fetchService.submit(this::fetchBoards);
    }
    public List<String> fetchBoards() {
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
        if(snap.size() == 0) return 0;
        int sum = 0;
        for(Integer n : snap) {
            if(n == null) break;
            sum += n;
        }
        return sum/snap.size();
    }

    Map<String, Cached<Future<Long>>> lastResetCache = new HashMap<>();

    public Future<Long> getLastReset(String board, TimedType type) {
        String key = board+type.toString();
        Cached<Future<Long>> cached = lastResetCache.getOrDefault(key, Cached.none());
        if(System.currentTimeMillis() - cached.getLastGet() < 30000) return cached.getThing();
        if(cached.getThing() != null && !cached.getThing().isDone()) {
            plugin.getLogger().warning("Not fetching new lastReset because the old one hasnt returned yet! (database overloaded?) " +
                    "(f: "+getFetching()+" avg: "+getFetchingAverage()+")");
            return cached.getThing();
        }
        CompletableFuture<Long> future = new CompletableFuture<>();
        long start = System.currentTimeMillis();
        if(plugin.isShuttingDown()) {
            future.complete(-1L);
            return future;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long lastReset = plugin.getCache().getLastReset(board, type)/1000;
            if(future.isDone()) return;
            future.complete(lastReset);
            long took = System.currentTimeMillis()-start;
            if(took > 500) {
                Debug.info("lastReset fetch took "+took+"ms");
            }
        });
        lastResetCache.put(key, new Cached<>(System.currentTimeMillis(), future));
        return future;

    }

    public int getActiveFetchers() {
        return fetchService.getActiveCount();
    }
    public int getMaxFetchers() {
        return fetchService.getMaximumPoolSize();
    }

    private void checkWrong() {
        if(fetching.get() > 5000) {
            plugin.getLogger().warning("Something might be going wrong, printing some useful info");
            Thread.dumpStack();
        }
    }

    public int cacheTime() {

        int r = 1000;

        int fetchingAverage = getFetchingAverage();

        if(fetchingAverage == Integer.MAX_VALUE) {
            return r;
        }

        if(fetchingAverage > 0) {
            r = 2000;
        }
        if(fetchingAverage >= 2) {
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
        if(fetchingAverage > 300) {
            r = 3600000;
        }
        if(fetchingAverage > 400) {
            r = 7200000;
        }


        return r;
    }

    public int getQueuedTasks() {
        return fetchService.getQueue().size();
    }

    public boolean boardExists(String board) {
        return getBoards().contains(board);
    }

    public Future<?> submit(Runnable task) {
        return fetchService.submit(task);
    }
}

