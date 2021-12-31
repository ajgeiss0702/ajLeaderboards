package us.ajg0702.leaderboards.cache.methods;

import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.utils.common.ConfigFile;

import java.io.File;
import java.sql.*;
import java.util.Locale;

public class SqliteMethod implements CacheMethod {
    private Connection conn;
    private LeaderboardPlugin plugin;
    private ConfigFile config;
    private Cache cacheInstance;
    @Override
    public Connection getConnection() {
        try {
            if(conn.isClosed()) {
                init(plugin, config, cacheInstance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    @Override
    public void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance) {
        this.plugin = plugin;
        this.config = config;
        this.cacheInstance = cacheInstance;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        String url = "jdbc:sqlite:"+plugin.getDataFolder().getAbsolutePath()+ File.separator+"cache.db";
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            plugin.getLogger().severe("Unnable to create cache file! The plugin will not work correctly!");
            e.printStackTrace();
            return;
        }
        try(Statement statement = conn.createStatement()) {
            ResultSet rs = statement.executeQuery("PRAGMA user_version;");
            int version = rs.getInt(1);
            rs.close();

            if(version == 0) {
                plugin.getLogger().info("Running table updater. (pv"+version+")");
                for(String b : cacheInstance.getBoards()) {
                    statement.executeUpdate("alter table '"+b+"' add column namecache TEXT;");
                    statement.executeUpdate("alter table '"+b+"' add column prefixcache TEXT;");
                    statement.executeUpdate("alter table '"+b+"' add column suffixcache TEXT;");
                }
                statement.executeUpdate("PRAGMA user_version = 1;");
            } else if(version == 1) {
                plugin.getLogger().info("Running SQLite table updater (pv"+version+")");

                for(String b : cacheInstance.getBoards()) {
                    for(TimedType typeEnum : TimedType.values()) {
                        if(typeEnum == TimedType.ALLTIME) continue;
                        String type = typeEnum.name().toLowerCase(Locale.ROOT);
                        statement.executeUpdate("alter table '"+b+"' add column '"+type+"_delta' NUMERIC");
                        statement.executeUpdate("alter table '"+b+"' add column '"+type+"_lasttotal' NUMERIC");
                        statement.executeUpdate("alter table '"+b+"' add column '"+type+"_timestamp' DATETIME");
                    }
                }

                statement.executeUpdate("PRAGMA user_version = 2;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close(Connection connection) {}
}
