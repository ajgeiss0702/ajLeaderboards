package us.ajg0702.leaderboards.cache;

import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.Connection;
import java.sql.SQLException;

public interface CacheMethod {
    Connection getConnection() throws SQLException;
    void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance);
    void close(Connection connection) throws SQLException;
    int getMaxConnections();
    void shutdown();
    String formatStatement(String s);
    String getName();
    boolean requiresClose();
}
