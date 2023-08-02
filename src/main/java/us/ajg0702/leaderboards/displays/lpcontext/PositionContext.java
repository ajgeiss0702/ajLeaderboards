package us.ajg0702.leaderboards.displays.lpcontext;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.keys.BoardType;
import us.ajg0702.leaderboards.boards.keys.PlayerBoardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PositionContext implements ContextCalculator<Player> {
    private final LeaderboardPlugin plugin;

    List<BoardType> contextBoardTypes = new ArrayList<>();

    public PositionContext(LeaderboardPlugin leaderboardPlugin) {
        plugin = leaderboardPlugin;

        calculatePotentialContexts();
    }

    LoadingCache<PlayerBoardType, Integer> positionCache = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .refreshAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build(new CacheLoader<PlayerBoardType, Integer>() {
                @Override
                public @NotNull Integer load(@NotNull PlayerBoardType key) {
                    return plugin.getTopManager().getStatEntry(key.getPlayer(), key.getBoard(), key.getType()).getPosition();
                }

                @Override
                public @NotNull ListenableFuture<Integer> reload(@NotNull PlayerBoardType key, @NotNull Integer oldValue) {
                    if(plugin.isShuttingDown()) {
                        return Futures.immediateFuture(oldValue);
                    }
                    if((plugin.getTopManager().getQueuedTasks() + plugin.getTopManager().getActiveFetchers()) > 50 || plugin.getTopManager().getFetchingAverage() > 75) {
                        return Futures.immediateFuture(oldValue);
                    }
                    ListenableFutureTask<Integer> task = ListenableFutureTask.create(
                            () -> plugin.getTopManager().getStatEntry(key.getPlayer(), key.getBoard(), key.getType()).getPosition()
                    );
                    if(plugin.isShuttingDown()) return Futures.immediateFuture(oldValue);
                    plugin.getTopManager().submit(task);
                    return task;
                }
            });

    @Override
    public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        for (BoardType contextBoardType : contextBoardTypes) {
            consumer.accept(
                    "ajlb_pos_"+contextBoardType.getBoard()+"_"+contextBoardType.getType().lowerName(),
                    positionCache.getUnchecked(new PlayerBoardType(
                            target, contextBoardType.getBoard(), contextBoardType.getType()
                    ))+""
            );
        }

    }

    private ContextSet potentialContexts;
    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        return potentialContexts;
    }

    public void calculatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        for (BoardType contextBoardType : contextBoardTypes) {
            for (int i = 1; i <= 10; i++) {
                builder.add("ajlb_pos_"+contextBoardType.getBoard()+"_"+contextBoardType.getType().lowerName(), i+"");
            }
        }

        potentialContexts = builder.build();
    }

    public void reloadConfig() {
        contextBoardTypes.clear();

        if(plugin.getAConfig().getStringList("only-register-lpc-for").isEmpty()) {
            plugin.getLogger().warning("Luckperms Contexts are enabled, but only-register-lpc-for has not been configured! Configuring only-register-lpc-for is strongly recommended to improve performance!");
            for (String board : plugin.getTopManager().getBoards()) {
                contextBoardTypes.addAll(allBoardTypes(board));
            }
        } else {
            List<String> btRaws = plugin.getAConfig().getStringList("only-register-lpc-for");
            for (String btRaw : btRaws) {
                String board = btRaw;
                if(board.contains(":")) {
                    int colonPosition = board.lastIndexOf(":");
                    board = board.substring(0, colonPosition);
                    String typeRaw = btRaw.substring(colonPosition + 1).toUpperCase(Locale.ROOT); // +1 to not include the colon

                    try {
                        TimedType type = TimedType.valueOf(typeRaw);
                        contextBoardTypes.add(new BoardType(board, type));
                    } catch(IllegalArgumentException e) {
                        // if the part after the colon is not a timed type, assume its part of the placeholder
                        Debug.info("[Context filter] Assuming" + btRaw + " is a board name! (the stuff after colon is not a timed type: '" + typeRaw + "'");
                        contextBoardTypes.addAll(allBoardTypes(btRaw));
                    }
                } else {
                    // if it doesn't have a :, treat it as a board and add all types from that board
                    contextBoardTypes.addAll(allBoardTypes(board));
                }
            }
        }

        calculatePotentialContexts();
    }

    private List<BoardType> allBoardTypes(String board) {
        List<BoardType> r = new ArrayList<>();
        for (TimedType type : TimedType.values()) {
            r.add(new BoardType(board, type));
        }
        return r;
    }
}
