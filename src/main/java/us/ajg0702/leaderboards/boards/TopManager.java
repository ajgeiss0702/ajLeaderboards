package us.ajg0702.leaderboards.boards;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;
import us.ajg0702.leaderboards.nms.ThreadFactoryProxy;
import us.ajg0702.leaderboards.utils.Cached;

import java.time.Duration;
import java.util.*;
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
        int t = method instanceof MysqlMethod ? Math.max(10, method.getMaxConnections()) : 100;
        fetchService = new ThreadPoolExecutor(
                t, t,
                10, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000000, true)
        );
        fetchService.allowCoreThreadTimeOut(true);
        fetchService.setThreadFactory(ThreadFactoryProxy.getDefaultThreadFactory("AJLBFETCH"));
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            rolling.add(getQueuedTasks()+getActiveFetchers());
            if(rolling.size() > 50) {
                rolling.remove(0);
            }
        }, 0, 2);
    }

    private final Map<BoardType, Map<Integer, Cached<StatEntry>>> cache = new HashMap<>();

    /**
     * Get a leaderboard position
     * @param position The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStat(int position, String board, TimedType type) {
        BoardType boardType = new BoardType(board, type);
        if(!cache.containsKey(boardType)) {
            cache.put(boardType, new HashMap<>());
        }

        if(cache.get(boardType).containsKey(position)) {
            if(System.currentTimeMillis() - cache.get(boardType).get(position).getLastGet() > cacheTime()) {
                cache.get(boardType).get(position).setLastGet(System.currentTimeMillis());
                fetchPositionAsync(position, boardType);
            }
            return cache.get(boardType).get(position).getThing();
        }

        cache.get(boardType).put(position, new Cached<>(StatEntry.loading(plugin, board, type)));
        if(plugin.getAConfig().getBoolean("blocking-fetch")) {
            return fetchPosition(position, boardType);
        } else {
            fetchPositionAsync(position, boardType);
            return StatEntry.loading(plugin, board, type);
        }
    }
    AtomicInteger fetching = new AtomicInteger(0);
    private void fetchPositionAsync(int position, BoardType boardType) {
        if(plugin.isShuttingDown()) return;
        if(!cache.get(boardType).containsKey(position)) {
            cache.get(boardType).put(position, new Cached<>(StatEntry.loading(plugin, boardType)));
        }
        checkWrong();
        fetchService.submit(() -> fetchPosition(position, boardType));
    }
    private StatEntry fetchPosition(int position, BoardType boardType) {
        int f = fetching.getAndIncrement();
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching ("+fetchService.getPoolSize()+") (pos): "+f);
        StatEntry te = plugin.getCache().getStat(position, boardType.getBoard(), boardType.getType());
        cache.get(boardType).put(position, new Cached<>(te));
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
            if(System.currentTimeMillis() - lastGetSE.get(board).get(type).get(player) > Math.max(cacheTime(), 5000)) {
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
            lastGetSE.get(board).get(type).put(player, System.currentTimeMillis());
            return new StatEntry(-2, board, "", player.getName(), player.getName(), player.getUniqueId(), "", 0, type);
        }
    }

    List<UUID> fetchingPlayers = new CopyOnWriteArrayList<>();
    public StatEntry getCachedStatEntry(OfflinePlayer player, String board, TimedType type) {
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
        StatEntry r = cacheSE.get(board).get(type).get(player);
        if(r == null && !fetchingPlayers.contains(player.getUniqueId())) {
            fetchingPlayers.add(player.getUniqueId());
            fetchStatEntryAsync(player, board, type);
        }
        return r;
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
        fetchingPlayers.remove(player.getUniqueId());
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
                lastGetBoard = System.currentTimeMillis();
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

    LoadingCache<BoardType, Long> lastResetCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(12))
            .refreshAfterWrite(Duration.ofSeconds(30))
            .build(new CacheLoader<BoardType, Long>() {
                @Override
                public @NotNull Long load(@NotNull BoardType key) {
                    long start = System.nanoTime();
                    long lastReset = plugin.getCache().getLastReset(key.getBoard(), key.getType())/1000;
                    long took = System.nanoTime() - start;
                    long tookms = took/1000000;
                    //if(tookms > 500) {
                        if(tookms < 5) {
                            Debug.info("lastReset fetch took " + tookms + "ms ("+took+"ns)");
                        } else {
                            Debug.info("lastReset fetch took " + tookms + "ms");
                        }
                    //}
                    return lastReset;
                }
            });

    public long getLastReset(String board, TimedType type) {
        return lastResetCache.getUnchecked(new BoardType(board, type));
    }


    Map<ExtraKey, Cached<String>> extraCache = new HashMap<>();
    public String getExtra(UUID id, String placeholder) {
        ExtraKey key = new ExtraKey(id, placeholder);
        Cached<String> cached = extraCache.get(key);
        if(cached == null) {
            if(plugin.getAConfig().getBoolean("blocking-fetch")) {
                return fetchExtra(id, placeholder);
            } else {
                extraCache.put(key, new Cached<>(System.currentTimeMillis(), StatEntry.LOADING));
                fetchExtraAsync(id, placeholder);
                return StatEntry.LOADING;
            }
        } else {
            if(System.currentTimeMillis() - cached.getLastGet() > cacheTime()) {
                cached.setLastGet(System.currentTimeMillis());
                fetchExtraAsync(id, placeholder);
            }
            return cached.getThing();
        }
    }
    public String fetchExtra(UUID id, String placeholder) {
        String value = plugin.getExtraManager().getExtra(id, placeholder);
        extraCache.put(new ExtraKey(id, placeholder), new Cached<>(System.currentTimeMillis(), value));
        return value;
    }
    public void fetchExtraAsync(UUID id, String placeholder) {
        fetchService.submit(() -> fetchExtra(id, placeholder));
    }

    public String getCachedExtra(UUID id, String placeholder) {
        Cached<String> r = extraCache.get(new ExtraKey(id, placeholder));
        if(r == null) {
            fetchExtraAsync(id, placeholder);
            return null;
        }
        return r.getThing();
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

        if(fetchingAverage == 0 && getActiveFetchers() == 0) {
            return 500;
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

    public List<Integer> getRolling() {
        return rolling;
    }

    public int getActiveFetchers() {
        return fetchService.getActiveCount();
    }
    public int getMaxFetchers() {
        return fetchService.getMaximumPoolSize();
    }

    public int getQueuedTasks() {
        return fetchService.getQueue().size();
    }

    public int getWorkers() {
        return fetchService.getPoolSize();
    }

    public boolean boardExists(String board) {
        return getBoards().contains(board);
    }

    public Future<?> submit(Runnable task) {
        return fetchService.submit(task);
    }
}

