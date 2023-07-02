package us.ajg0702.leaderboards.boards;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
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
import us.ajg0702.leaderboards.cache.BlockingFetch;
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
    public TopManager(LeaderboardPlugin pl, List<String> initialBoards) {
        plugin = pl;
        CacheMethod method = plugin.getCache().getMethod();
        int t = method instanceof MysqlMethod ? Math.max(10, method.getMaxConnections()) : plugin.getAConfig().getInt("max-fetching-threads");
        int k = plugin.getAConfig().getInt("fetching-thread-pool-keep-alive");
        fetchService = new ThreadPoolExecutor(
                t, t,
                k, TimeUnit.MILLISECONDS,
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

        boardCache = initialBoards;
    }

    Map<PositionBoardType, Long> positionLastRefresh = new HashMap<>();
    List<PositionBoardType> positionFetching = new CopyOnWriteArrayList<>();
    LoadingCache<PositionBoardType, StatEntry> positionCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .refreshAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(10000)
            .removalListener(notification -> {
                if(!notification.getCause().equals(RemovalCause.REPLACED)) positionLastRefresh.remove((PositionBoardType) notification.getKey());
            })
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
            if(BlockingFetch.shouldBlock(plugin)) {
                cached = positionCache.getUnchecked(key);
            } else {
                if(!positionFetching.contains(key)) {
                    if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Starting fetch on " + key);
                    positionFetching.add(key);
                    fetchService.submit(() -> {
                        positionCache.getUnchecked(key);
                        positionFetching.remove(key);
                        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetch finished on " + key);
                    });
                }
                if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Returning loading for " + key);
                cacheStatPosition(position, new BoardType(board, type), null);
                return StatEntry.loading(plugin, position, board, type);
            }
        }

        cacheStatPosition(position, new BoardType(board, type), cached.playerID);

        return cached;
    }

    public final Map<UUID, Map<BoardType, Integer>> positionPlayerCache = new ConcurrentHashMap<>();

    private void cacheStatPosition(int position, BoardType boardType, UUID playerUUID) {
        for (Map.Entry<UUID, Map<BoardType, Integer>> entry : positionPlayerCache.entrySet()) {
            if(entry.getKey().equals(playerUUID)) continue;
            entry.getValue().remove(boardType, position);
        }

        if(playerUUID == null) return;

        Map<BoardType, Integer> newMap = positionPlayerCache.getOrDefault(playerUUID, new HashMap<>());

        newMap.put(boardType, position);

        positionPlayerCache.put(playerUUID, newMap);
    }

    Map<PlayerBoardType, Long> statEntryLastRefresh = new HashMap<>();
    LoadingCache<PlayerBoardType, StatEntry> statEntryCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.HOURS)
            .refreshAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(10000)
            .removalListener(notification -> {
                if(!notification.getCause().equals(RemovalCause.REPLACED)) statEntryLastRefresh.remove((PlayerBoardType) notification.getKey());
            })
            .build(new CacheLoader<PlayerBoardType, StatEntry>() {
                @Override
                public @NotNull StatEntry load(@NotNull PlayerBoardType key) {
                    return plugin.getCache().getStatEntry(key.getPlayer(), key.getBoard(), key.getType());
                }

                @Override
                public @NotNull ListenableFuture<StatEntry> reload(@NotNull PlayerBoardType key, @NotNull StatEntry oldValue) {
                    if(plugin.isShuttingDown() || System.currentTimeMillis() - statEntryLastRefresh.getOrDefault(key, 0L) < Math.max(cacheTime()*1.5, 5000)) {
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
            if(BlockingFetch.shouldBlock(plugin)) {
                cached = statEntryCache.getUnchecked(key);
            } else {
                fetchService.submit(() -> statEntryCache.getUnchecked(key));
                return StatEntry.loading(plugin, player, key.getBoardType());
            }
        }

        return cached;

    }

    public StatEntry getCachedStatEntry(OfflinePlayer player, String board, TimedType type) {
        return getCachedStatEntry(player, board, type, true);
    }
    public StatEntry getCachedStatEntry(OfflinePlayer player, String board, TimedType type, boolean fetchIfAbsent) {
        PlayerBoardType key = new PlayerBoardType(player, board, type);

        StatEntry r = statEntryCache.getIfPresent(key);
        if(fetchIfAbsent && r == null) {
            fetchService.submit(() -> statEntryCache.getUnchecked(key));
        }
        return r;
    }

    public StatEntry getCachedStat(int position, String board, TimedType type) {
        return getCachedStat(new PositionBoardType(position, board, type));
    }
    public StatEntry getCachedStat(PositionBoardType positionBoardType) {
        StatEntry r = positionCache.getIfPresent(positionBoardType);
        if(r == null) {
            fetchService.submit(() -> positionCache.getUnchecked(positionBoardType));
        }
        return r;
    }


    Map<String, Long> boardSizeLastRefresh = new HashMap<>();
    LoadingCache<String, Integer> boardSizeCache = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .refreshAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(10000)
            .removalListener(notification -> {
                if(!notification.getCause().equals(RemovalCause.REPLACED)) boardSizeLastRefresh.remove((String) notification.getKey());
            })
            .build(new CacheLoader<String, Integer>() {
                @Override
                public @NotNull Integer load(@NotNull String key) {
                    return plugin.getCache().getBoardSize(key);
                }

                @Override
                public @NotNull ListenableFuture<Integer> reload(@NotNull String key, @NotNull Integer oldValue) {
                    if(plugin.isShuttingDown() || System.currentTimeMillis() - boardSizeLastRefresh.getOrDefault(key, 0L) < Math.max(cacheTime()*2, 5000)) {
                        return Futures.immediateFuture(oldValue);
                    }
                    ListenableFutureTask<Integer> task = ListenableFutureTask.create(() -> {
                        boardSizeLastRefresh.put(key, System.currentTimeMillis());
                        return plugin.getCache().getBoardSize(key);
                    });
                    if(plugin.isShuttingDown()) return Futures.immediateFuture(oldValue);
                    fetchService.execute(task);
                    return task;
                }
            });


    /**
     * Get the size of a leaderboard (number of players)
     * @param board The board
     * @return The number of players in that board
     */
    public int getBoardSize(String board) {
        Integer cached = boardSizeCache.getIfPresent(board);

        if(cached == null) {
            if(BlockingFetch.shouldBlock(plugin)) {
                cached = boardSizeCache.getUnchecked(board);
            } else {
                fetchService.submit(() -> boardSizeCache.getUnchecked(board));
                return -2;
            }
        }

        return cached;

    }


    List<String> boardCache;
    long lastGetBoard = 0;
    public List<String> getBoards() {
        if(boardCache == null) {
            if(BlockingFetch.shouldBlock(plugin)) {
                return fetchBoards();
            } else {
                if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("need to fetch boards");
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
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Finished fetching boards");
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
            if(BlockingFetch.shouldBlock(plugin)) {
                return fetchExtra(id, placeholder);
            } else {
                extraCache.put(key, new Cached<>(System.currentTimeMillis(), plugin.getMessages().getRawString("loading.text")));
                fetchExtraAsync(id, placeholder);
                return plugin.getMessages().getRawString("loading.text");
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

        if(position < 1) {
            return StatEntry.noRelData(plugin, position, board, type);
        }

        return getStat(position, board, type);
    }


    private void checkWrong() {
        if(fetching.get() > 5000) {
            plugin.getLogger().warning("Something might be going wrong, printing some useful info");
            Thread.dumpStack();
        }
    }

    long lastLargeAverage = 0;

    public int cacheTime() {

        boolean recentLargeAverage = System.currentTimeMillis() - lastLargeAverage < 30000;

        int r = recentLargeAverage ? 5000 : 1000;

        int fetchingAverage = getFetchingAverage();

        if(fetchingAverage == Integer.MAX_VALUE) {
            return r;
        }

        int activeFetchers = getActiveFetchers();
        int totalTasks = activeFetchers + getQueuedTasks();

        if(!recentLargeAverage) {
            if(fetchingAverage == 0 && activeFetchers == 0) {
                return 500;
            }
            if(fetchingAverage > 0) {
                r = 2000;
            }
            if(fetchingAverage >= 2) {
                r = 5000;
            }
        }
        if((fetchingAverage >= 5 || totalTasks > 25) && activeFetchers > 0) {
            r = 10000;
        }
        if((fetchingAverage > 10 || totalTasks > 59) && activeFetchers > 0) {
            r = 15000;
        }
        if((fetchingAverage > 20 || totalTasks > 75) && activeFetchers > 0) {
            r = 30000;
        }
        if((fetchingAverage > 30 || totalTasks > 100) && activeFetchers > 0) {
            r = 60000;
        }
        if(fetchingAverage > 50 || totalTasks > 125) {
            lastLargeAverage = System.currentTimeMillis();
            if(activeFetchers > 0) {
                r = 120000;
            }
        }
        if((fetchingAverage > 100 || totalTasks > 150) && activeFetchers > 0) {
            r = 180000;
        }
        if((fetchingAverage > 300 || totalTasks > 175) && activeFetchers > 0) {
            r = 3600000;
        }
        if((fetchingAverage > 400 || totalTasks > 200) && activeFetchers > 0) {
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
        boolean result = getBoards().contains(board);
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Checking " + board + ": " + result);
        if(!result) {
            if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Boards: " + String.join(", ", getBoards()));
        }
        return result;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Future<?> submit(Runnable task) {
        return fetchService.submit(task);
    }
}

