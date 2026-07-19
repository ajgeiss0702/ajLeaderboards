package us.ajg0702.leaderboards.boards;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
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

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TopManager {

    private final ThreadPoolExecutor fetchService;
    //private final ThreadPoolExecutor fetchService = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private final AtomicInteger fetching = new AtomicInteger(0);

    public void shutdown(boolean fast) {
        fetchService.getQueue().clear(); // clear queue so we dont wait for all tasks in the queue to run
        fetchService.shutdown();
        try {
            fetchService.awaitTermination(fast ? 2 : 15, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }

    private static final String OUT_OF_THREADS_MESSAGE = "unable to create native thread: possibly out of memory or process/resource limits reached";


    private final LeaderboardPlugin plugin;
    public TopManager(LeaderboardPlugin pl, List<String> initialBoards) {
        plugin = pl;
        CacheMethod method = plugin.getCache().getMethod();
        int t = method instanceof MysqlMethod ? Math.max(10, method.getMaxConnections()) : plugin.getAConfig().getInt("max-fetching-threads");
        int keepAlive = plugin.getAConfig().getInt("fetching-thread-pool-keep-alive");
        ThreadFactory threadFactory;
        try {
            // Check if virtual thread methods exist (Java 21+)
            Object builder = Thread.class.getMethod("ofVirtual").invoke(null);
            Class<?> builderInterface = Class.forName("java.lang.Thread$Builder$OfVirtual");
            builderInterface.getMethod("name", String.class, Long.TYPE)
                    .invoke(builder, "AJLBFETCH", 0L);
            threadFactory = (ThreadFactory) builderInterface.getMethod("factory")
                    .invoke(builder);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            plugin.getLogger().info("Using old thread pool due to running on an older Java version! If possible, updating to Java 21+ is recommended.");
            Debug.info(e.getClass().getName() + ": " + e.getMessage());
            // Fallback to Java 11/17 logic
            threadFactory = ThreadFactoryProxy.getDefaultThreadFactory("AJLBFETCH");
        }
        fetchService = new ThreadPoolExecutor(
                t, t,
                keepAlive, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000000),
                threadFactory
        );
        fetchService.allowCoreThreadTimeOut(true);

        plugin.getScheduledExecutorService().scheduleAtFixedRate(() -> {
            if(plugin.isShuttingDown()) return;
            rolling.add(getQueuedTasks()+getActiveFetchers());
            if(rolling.size() > 50) {
                rolling.remove(0);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        boardCache = initialBoards;
    }

    Map<PositionBoardType, Long> positionLastRefresh = new ConcurrentHashMap<>();
    Set<PositionBoardType> positionFetching = ConcurrentHashMap.newKeySet();
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
        StatEntry cached;

        try {
            cached = positionCache.getIfPresent(key);
            if (cached == null) {
                if (BlockingFetch.shouldBlock(plugin)) {
                    cached = positionCache.getUnchecked(key);
                } else {
                if (positionFetching.add(key)) {
                    if (plugin.isShuttingDown()) return StatEntry.loading(plugin, position, board, type);
                    if (plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Starting fetch on " + key);
                    fetchService.submit(() -> {
                        positionCache.getUnchecked(key);
                        positionFetching.remove(key);
                        if (plugin.getAConfig().getBoolean("fetching-de-bug"))
                            Debug.info("Fetch finished on " + key);
                    });
                }
                    if (plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Returning loading for " + key);
                    return StatEntry.loading(plugin, position, board, type);
                }
            }
        } catch(Exception e) {
            String message = e.getMessage();
            if(message != null && message.contains(OUT_OF_THREADS_MESSAGE)) {
                informAboutThreadLimit();
                return StatEntry.error(position, board, type);
            } else {
                throw e;
            }
        }

        cacheStatPosition(position, new BoardType(board, type), cached.playerID);

        return cached;
    }

    public final Map<UUID, Map<BoardType, Integer>> positionPlayerCache = new ConcurrentHashMap<>();

    private void cacheStatPosition(int position, BoardType boardType, UUID playerUUID) {
        if(playerUUID == null) return;

        Integer oldPosition = positionPlayerCache.get(playerUUID) != null 
            ? positionPlayerCache.get(playerUUID).get(boardType) 
            : null;
        
        if(oldPosition != null) {
            int minPos = Math.min(oldPosition, position);
            int maxPos = Math.max(oldPosition, position);
            
            if(minPos != maxPos) {
                positionPlayerCache.forEach((uuid, map) -> {
                    if(!uuid.equals(playerUUID)) {
                        Integer cachedPos = map.get(boardType);
                        if(cachedPos != null && cachedPos >= minPos && cachedPos <= maxPos) {
                            map.remove(boardType);
                        }
                    }
                });
            }
        } else {
            positionPlayerCache.forEach((uuid, map) -> {
                if(!uuid.equals(playerUUID)) {
                    map.remove(boardType, position);
                }
            });
        }

        positionPlayerCache.compute(playerUUID, (uuid, existingMap) -> {
            Map<BoardType, Integer> map = existingMap != null ? existingMap : new ConcurrentHashMap<>();
            map.put(boardType, position);
            return map;
        });

        if(positionPlayerCache.size() > 10000) {
            Iterator<UUID> it = positionPlayerCache.keySet().iterator();
            while(positionPlayerCache.size() > 10000 && it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    Map<PlayerBoardType, Long> statEntryLastRefresh = new ConcurrentHashMap<>();
    LoadingCache<PlayerBoardType, StatEntry> statEntryCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .refreshAfterWrite(5, TimeUnit.SECONDS)
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
                    long msSinceRefresh = System.currentTimeMillis() - statEntryLastRefresh.getOrDefault(key, 0L);
                    double cacheTime = Math.max(cacheTime()*1.5, plugin.getAConfig().getInt("min-player-cache-time"));
                    // The cache time is randomized a bit so that players are spread out more
                    if(plugin.isShuttingDown() || msSinceRefresh < (cacheTime + ((cacheTime / 2) * Math.random()))) {
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

        try {
            StatEntry cached = statEntryCache.getIfPresent(key);

            if (cached == null) {
                if (BlockingFetch.shouldBlock(plugin)) {
                    cached = statEntryCache.getUnchecked(key);
                } else {
                    if (plugin.isShuttingDown()) return StatEntry.loading(player, key.getBoardType());
                    fetchService.submit(() -> statEntryCache.getUnchecked(key));
                    return StatEntry.loading(player, key.getBoardType());
                }
            }

            return cached;
        } catch(Exception e) {
            String message = e.getMessage();
            if(message != null && message.contains(OUT_OF_THREADS_MESSAGE)) {
                informAboutThreadLimit();
                return StatEntry.error(-4, board, type);
            } else {
                throw e;
            }
        }
    }

    public StatEntry getCachedStatEntry(OfflinePlayer player, String board, TimedType type) {
        return getCachedStatEntry(player, board, type, true);
    }
    public StatEntry getCachedStatEntry(OfflinePlayer player, String board, TimedType type, boolean fetchIfAbsent) {
        PlayerBoardType key = new PlayerBoardType(player, board, type);

        StatEntry r;
        try {
            r = statEntryCache.getIfPresent(key);
            if(fetchIfAbsent && r == null) {
                if (plugin.isShuttingDown()) return null;
                fetchService.submit(() -> statEntryCache.getUnchecked(key));
            }
        } catch(Exception e) {
            String message = e.getMessage();
            if(message != null && message.contains(OUT_OF_THREADS_MESSAGE)) {
                informAboutThreadLimit();
                return StatEntry.error(-4, board, type);
            } else {
                throw e;
            }
        }
        return r;
    }

    public StatEntry getCachedStat(int position, String board, TimedType type) {
        return getCachedStat(new PositionBoardType(position, board, type), true);
    }
    public StatEntry getCachedStat(PositionBoardType positionBoardType, boolean fetchIfAbsent) {
        StatEntry r;
        try {
            r = positionCache.getIfPresent(positionBoardType);
            if (r == null && fetchIfAbsent) {
                if (plugin.isShuttingDown()) return null;
                fetchService.submit(() -> positionCache.getUnchecked(positionBoardType));
            }
        } catch(Exception e) {
            String message = e.getMessage();
            if(message != null && message.contains(OUT_OF_THREADS_MESSAGE)) {
                informAboutThreadLimit();
                return StatEntry.error(positionBoardType.getPosition(), positionBoardType.getBoard(), positionBoardType.getType());
            } else {
                throw e;
            }
        }
        return r;
    }


    Map<String, Long> boardSizeLastRefresh = new ConcurrentHashMap<>();
    LoadingCache<String, Integer> boardSizeCache = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .refreshAfterWrite(15, TimeUnit.SECONDS)
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

        try {
            Integer cached = boardSizeCache.getIfPresent(board);

            if (cached == null) {
                if (BlockingFetch.shouldBlock(plugin)) {
                    cached = boardSizeCache.getUnchecked(board);
                } else {
                    if (plugin.isShuttingDown()) return -2;
                    fetchService.submit(() -> boardSizeCache.getUnchecked(board));
                    return -2;
                }
            }

            return cached;
        } catch(Exception e) {
            String message = e.getMessage();
            if(message != null && message.contains(OUT_OF_THREADS_MESSAGE)) {
                informAboutThreadLimit();
                return -4;
            } else {
                throw e;
            }
        }

    }

    Map<BoardType, Long> totalLastRefresh = new ConcurrentHashMap<>();
    LoadingCache<BoardType, Double> totalCache = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .refreshAfterWrite(15, TimeUnit.SECONDS)
            .maximumSize(10000)
            .removalListener(notification -> {
                if(!notification.getCause().equals(RemovalCause.REPLACED)) totalLastRefresh.remove((BoardType) notification.getKey());
            })
            .build(new CacheLoader<BoardType, Double>() {
                @Override
                public @NotNull Double load(@NotNull BoardType key) {
                    return plugin.getCache().getTotal(key.getBoard(), key.getType());
                }

                @Override
                public @NotNull ListenableFuture<Double> reload(@NotNull BoardType key, @NotNull Double oldValue) {
                    if(plugin.isShuttingDown() || System.currentTimeMillis() - totalLastRefresh.getOrDefault(key, 0L) < Math.max(cacheTime()*2, 5000)) {
                        return Futures.immediateFuture(oldValue);
                    }
                    ListenableFutureTask<Double> task = ListenableFutureTask.create(() -> {
                        totalLastRefresh.put(key, System.currentTimeMillis());
                        return plugin.getCache().getTotal(key.getBoard(), key.getType());
                    });
                    if(plugin.isShuttingDown()) return Futures.immediateFuture(oldValue);
                    fetchService.execute(task);
                    return task;
                }
            });


    /**
     * Gets the sum of all players on the leaderboard
     * @param board the board
     * @param type the timed type
     * @return the sum of all players in the specified board for the specified timed type
     */
    public double getTotal(String board, TimedType type) {
        BoardType boardType = new BoardType(board, type);

        try {
            Double cached = totalCache.getIfPresent(boardType);

            if (cached == null) {
                if (BlockingFetch.shouldBlock(plugin)) {
                    cached = totalCache.getUnchecked(boardType);
                } else {
                    if (plugin.isShuttingDown()) return -2;
                    fetchService.submit(() -> totalCache.getUnchecked(boardType));
                    return -2;
                }
            }

            return cached;
        } catch(Exception e) {
            String message = e.getMessage();
            if(message != null && message.contains(OUT_OF_THREADS_MESSAGE)) {
                informAboutThreadLimit();
                return -4;
            } else {
                throw e;
            }
        }

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
        if (plugin.isShuttingDown()) return;
        checkWrong();
        fetchService.submit(this::fetchBoards);
    }
    public List<String> fetchBoards() {
        int f = fetching.getAndIncrement();
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Fetching (boards): "+f);
        boardCache = plugin.getCache().getBoards();
        if(plugin.getAConfig().getBoolean("fetching-de-bug")) Debug.info("Finished fetching boards");
        removeFetching();
        return boardCache;
    }

    List<Integer> rolling = new CopyOnWriteArrayList<>();
    private void removeFetching() {
        fetching.decrementAndGet();
    }

    public int getFetching() {
        return fetching.get();
    }

    public int getFetchingAverage() {
        List<Integer> snap = new ArrayList<>(rolling);
        if(snap.isEmpty()) return 0;
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


    Map<ExtraKey, Long> extraLastRefresh = new ConcurrentHashMap<>();
    LoadingCache<ExtraKey, Optional<String>> extraCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build(new CacheLoader<ExtraKey, Optional<String>>() {
                @Override
                public @NonNull Optional<String> load(@NonNull ExtraKey key) {
                    return Optional.ofNullable(plugin.getExtraManager().getExtra(key.getId(), key.getPlaceholder()));
                }
            });
    public String getExtra(UUID id, String placeholder) {
        ExtraKey key = new ExtraKey(id, placeholder);
        Optional<String> cached = extraCache.getIfPresent(key);
        if(cached == null) {
            if(BlockingFetch.shouldBlock(plugin)) {
                return fetchExtra(id, placeholder);
            } else {
                extraCache.put(key, Optional.of(plugin.getMessages().getRawString("loading.text")));
                fetchExtraAsync(id, placeholder);
                return plugin.getMessages().getRawString("loading.text");
            }
        } else {
            long lastRefresh = extraLastRefresh.getOrDefault(key, 0L);
            if(System.currentTimeMillis() - lastRefresh > cacheTime()) {
                extraLastRefresh.put(key, System.currentTimeMillis());
                fetchExtraAsync(id, placeholder);
            }
            return cached.orElse(null);
        }
    }
    public String fetchExtra(UUID id, String placeholder) {
        ExtraKey key = new ExtraKey(id, placeholder);
        String value = plugin.getExtraManager().getExtra(id, placeholder);
        extraCache.put(key, Optional.ofNullable(value));
        return value;
    }
    public void fetchExtraAsync(UUID id, String placeholder) {
        if (plugin.isShuttingDown()) return;
        fetchService.submit(() -> fetchExtra(id, placeholder));
    }

    public String getCachedExtra(UUID id, String placeholder) {
        Optional<String> r = extraCache.getIfPresent(new ExtraKey(id, placeholder));
        if(r == null) {
            fetchExtraAsync(id, placeholder);
            return null;
        }
        return r.orElse(null);
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
        boolean moreFetching = plugin.getAConfig().getBoolean("more-fetching");


        int r = moreFetching ? (recentLargeAverage ? 5_000 : 1_000) : 20_000;

        int fetchingAverage = getFetchingAverage();

        if(fetchingAverage == Integer.MAX_VALUE) {
            return r;
        }

        int activeFetchers = getActiveFetchers();
        int totalTasks = activeFetchers + getQueuedTasks();

        if(moreFetching) {
            if(!recentLargeAverage) {
                if(fetchingAverage == 0 && activeFetchers == 0) {
                    return 500;
                }
                if(fetchingAverage > 0) {
                    r = 2_000;
                }
                if(fetchingAverage >= 2) {
                    r = 5_000;
                }
            }
            if((fetchingAverage >= 3 || totalTasks > 10) && activeFetchers > 0) {
                r = 10_000;
            }
            if((fetchingAverage > 5 || totalTasks > 15) && activeFetchers > 0) {
                r = 15_000;
            }
        }
        if((fetchingAverage > 7 || totalTasks > 20) && activeFetchers > 0) {
            r = 30_000;
        }
        if((fetchingAverage > 10 || totalTasks > 25) && activeFetchers > 0) {
            r = 60_000;
        }
        if(fetchingAverage > 20 || totalTasks > 30) {
            lastLargeAverage = System.currentTimeMillis();
            if(activeFetchers > 0) {
                r = 120_000;
            }
        }
        if((fetchingAverage > 35 || totalTasks > 50) && activeFetchers > 0) {
            r = 180_000;
        }
        if((fetchingAverage > 50 || totalTasks > 75) && activeFetchers > 0) {
            r = 3_600_000; // 1 hour
        }
        if((fetchingAverage > 100 || totalTasks > 150) && activeFetchers > 0) {
            r = 7_200_000; // 2 hours
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
        if (plugin.isShuttingDown()) return null;
        return fetchService.submit(task);
    }


    private void informAboutThreadLimit() {
        plugin.getLogger().warning("'" + OUT_OF_THREADS_MESSAGE + "' error detected! " +
                "This is usually caused by your server hitting the limit on the number of threads it can have. " +
                "If the server crashes, take the crash report and paste it into https://crash-report-analyser.ajg0702.us/ " +
                "to help find which plugin is using too many threads. If you need help interpreting the results, " +
                "feel free to ask in aj's discord server (invite link is on the ajLeaderboards plugin page under 'support')");
    }
}

