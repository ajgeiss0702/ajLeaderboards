package us.ajg0702.leaderboards.cache;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.helpers.DbRow;
import us.ajg0702.utils.common.ConfigFile;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface CacheMethod {
    void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance);

    void shutdown();

    String getName();

    boolean requiresClose();

    StatEntry getStatEntry(int position, String board, TimedType type) throws SQLException;

    @Nullable
    StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type);

    int getBoardSize(String board);

    double getLastTotal(String board, OfflinePlayer player, TimedType type);

    long getLastReset(String board, TimedType type);

    void resetBoard(String board, TimedType type, long newTime);

    void insertRows(String board, List<DbRow> rows) throws SQLException;

    List<DbRow> getRows(String board) throws SQLException;

    boolean createBoard(String name);

    boolean removePlayer(String board, String playerName);

    void upsertPlayer(String board, OfflinePlayer player, double output, String prefix, String suffix, String displayName);

    boolean removeBoard(String board);

    List<String> getDbTableList();

    List<String> getBoards();

    String getExtra(UUID id, String placeholder);

    void upsertExtra(UUID id, String placeholder, String value);

    void createExtraTable();
}
