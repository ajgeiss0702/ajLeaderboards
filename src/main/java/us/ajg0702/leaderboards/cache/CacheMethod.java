package us.ajg0702.leaderboards.cache;

import us.ajg0702.leaderboards.Main;
import us.ajg0702.utils.spigot.ConfigFile;

import java.sql.Connection;
import java.sql.SQLException;

public interface CacheMethod {
    Connection getConnection() throws SQLException;
    void init(Main plugin, ConfigFile config);
    void close(Connection connection) throws SQLException;
}
