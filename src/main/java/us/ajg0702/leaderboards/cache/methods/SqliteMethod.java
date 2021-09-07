package us.ajg0702.leaderboards.cache.methods;

import us.ajg0702.leaderboards.Main;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.utils.spigot.ConfigFile;

import java.io.File;
import java.sql.*;

public class SqliteMethod implements CacheMethod {
    private Connection conn;
    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public void init(Main plugin, ConfigFile config, Cache cacheInstance) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close(Connection connection) {

    }
}
