package us.ajg0702.leaderboards.boards;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.keys.BoardType;
import us.ajg0702.leaderboards.boards.keys.ExtraKey;
import us.ajg0702.leaderboards.boards.keys.PlayerBoardType;
import us.ajg0702.leaderboards.boards.keys.PositionBoardType;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;
import us.ajg0702.leaderboards.nms.legacy.ThreadFactoryProxy;
import us.ajg0702.leaderboards.utils.Cached;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TopManager {

    private final ThreadPoolExecutor fetchService;
    //private final ThreadPoolExecutor fetchService = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private AtomicInteger fetching = new AtomicInteger(0);

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

    Map<PositionBoardType, Long> positionLastRefresh = new HashMap<>();
    List<PositionBoardType> positionFetching = new CopyOnWriteArrayList<>();
    LoadingCache<PositionBoardType, StatEntry> positionCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .refreshAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(10000)
            .removalListener(notification -> positionLastRefresh.remove((PositionBoardType) notification.getKey()))
            .build(new CacheLoader<PositionBoardType, StatEntry>() {
                @Override
                public @NotNull StatEntry load(@NotNull PositionBoardType key) {
                    return plugin.getCache().getStat(key.getPosition(), key.getBoard(), key.getType());
                }

                @Override
                public @NotNull ListenableFuture<StatEntry> reload(@NotNull PositionBoardType key, @NotNull StatEntry oldValue) {
                    if(plugin.isShuttingDown() || System.currentTimeMillis() - positionLastRefresh.getOrDefault(key, 0L) < cacheTime()) {
                        return Futures.immediateFuture(oldValue);
                    }
                    ListenableFutureTask<StatEntry> task = ListenableFutureTask.create(() -> {
                        positionLastRefresh.put(key, System.currentTimeMillis());
                        return plugin.getCache().getStat(key.getPosition(), key.getBoard(), key.getType());
                    });
                    if(plugin.isShuttingDown()) return Futures.immediateFuture(oldValue);
                    fetchService.execute(task);
                    return task;
                }
            });

    int b = 0;
    /**
     * Get a leaderboard position
     * @param position The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStat(int position, String board, TimedType type) {
        PositionBoardType key = new PositionBoardType(position, board, type);
        StatEntry cached = positionCache.getIfPresent(key);

        if(cached == null) {
            if(plugin.getAConfig().getBoolean("blocking-fetch")) {
                cached = positionCache.getUnchecked(key);
            } else {
                if(!positionFetching.contains(key)) {
                    positionFetching.add(key);
                    fetchService.submit(() -> {
                        positionCache.getUnchecked(key);
                        positionFetching.remove(key);
                    });
                }
                return StatEntry.loading(plugin, position, board, type);
            }
        }

        return cached;
    }

    Map<PlayerBoardType, Long> statEntryLastRefresh = new HashMap<>();
    LoadingCache<PlayerBoardType, StatEntry> statEntryCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.HOURS)
            .refreshAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(10000)
            .removalListener(notification -> statEntryLastRefresh.remove((PlayerBoardType) notification.getKey()))
            .build(new CacheLoader<PlayerBoardType, StatEntry>() {
                @Override
                public @NotNull StatEntry load(@NotNull PlayerBoardType key) {
                    return plugin.getCache().getStatEntry(key.getPlayer(), key.getBoard(), key.getType());
                }

                @Override
                public @NotNull ListenableFuture<StatEntry> reload(@NotNull PlayerBoardType key, @NotNull StatEntry oldValue) {
                    if(plugin.isShuttingDown() || System.currentTimeMillis() - statEntryLastRefresh.getOrDefault(key, 0L) < Math.max(cacheTime(), 5000)) {
                        return Futures.immediateFuture(oldValue);
                    }
                    ListenableFutureTask<StatEntry> task = ListenableFutureTask.create(() -> {
                        statEntryLastRefresh.put(key, System.currentTimeMillis());
                        return plugin.getCache().getStatEntry(key.getPlayer(), key.getBoard(), key.getType());
                    });
                    if(plugin.isShuttingDown()) return Futures.immediateFuture(oldValue);
                    fetchService.execute(task);
                    return task;
                }
            });

    /**
     * Get a leaderboard position
     * @param player The position to get
     * @param board The board
     * @return The StatEntry representing the position on the board
     */
    public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
        PlayerBoardType key = new PlayerBoardType(player, board, type);
        StatEntry cached = statEntryCache.getIfPresent(key);

        if(cached == null) {
            if(plugin.getAConfig().getBoolean("blocking-fetch")) {
                cached = statEntryCache.getUnchecked(key);
            } else {
                fetchService.submit(() -> statEntryCache.getUnchecked(key));
                return StatEntry.loading(plugin, player, key.getBoardType());
            }
        }

        return cached;

    }

    public StatEntry getCachedStatEntry(OfflinePlayer player, String board, TimedType type) {
        PlayerBoardType key = new PlayerBoardType(player, board, type);

        StatEntry r = statEntryCache.getIfPresent(key);
        if(r == null) {
            fetchService.submit(() -> statEntryCache.getUnchecked(key));
        }
        return r;
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
            .expireAfterAccess(12, TimeUnit.HOURS)
            .refreshAfterWrite(30, TimeUnit.SECONDS)
            .build(new CacheLoader<BoardType, Long>() {
                @Override
                public @NotNull Long load(@NotNull BoardType key) {
                    long start = System.nanoTime();
                    long lastReset = plugin.getCache().getLastReset(key.getBoard(), key.getType())/1000;
                    long took = System.nanoTime() - start;
                    long tookms = took/1000000;
                    if(tookms > 500) {
                        /*if(tookms < 5) {
                            Debug.info("lastReset fetch took " + tookms + "ms ("+took+"ns)");
                        } else {*/
                            Debug.info("lastReset fetch took " + tookms + "ms");
                        //}
                    }
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

    public StatEntry getRelative(OfflinePlayer player, int difference, String board, TimedType type) {
        StatEntry playerStatEntry = getCachedStatEntry(player, board, type);
        if(playerStatEntry == null || !playerStatEntry.hasPlayer()) {
            return StatEntry.loading(plugin, board, type);
        }
        int position = playerStatEntry.getPosition() + difference;

        return getStat(position, board, type);
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

    @SuppressWarnings("UnusedReturnValue")
    public Future<?> submit(Runnable task) {
        return fetchService.submit(task);
    }
}

