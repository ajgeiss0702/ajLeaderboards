package us.ajg0702.leaderboards.cache.methods;

import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.SQLCacheMethod;
import us.ajg0702.leaderboards.utils.UnClosableConnection;
import us.ajg0702.utils.common.ConfigFile;

import java.io.File;
import java.sql.*;
import java.util.Locale;

public class SqliteMethod extends SQLCacheMethod {
    private Connection conn;
    private LeaderboardPlugin plugin;
    private ConfigFile config;
    private Cache cacheInstance;

    public SqliteMethod(ConfigFile storageConfig) {
        super(storageConfig);
    }

    @Override
    public Connection getConnection() {
        try {
            if (conn.isClosed()) {
                plugin.getLogger().warning("Sqlite connection is dead, making a new one");
                init(plugin, config, cacheInstance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new UnClosableConnection(conn);
    }

    @Override
    public void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance) {
        plugin.getLogger().warning(
                "SQLite is not recommended! Please switch to h2 for a faster (and more stable) cache storage. " +
                        "SQLite support will be removed in the future. " +
                        "See how to switch without losing data here: https://wiki.ajg0702.us/ajleaderboards/moving-storage-methods"
        );
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
                version = 1;
            }
            if(version == 1) {
                plugin.getLogger().info("Running SQLite table updater (pv"+version+")");

                for(String b : cacheInstance.getDbTableList()) {
                    for(TimedType typeEnum : TimedType.values()) {
                        if(typeEnum == TimedType.ALLTIME) continue;
                        String type = typeEnum.name().toLowerCase(Locale.ROOT);
                        statement.executeUpdate("alter table '"+b+"' add column '"+type+"_delta' NUMERIC");
                        statement.executeUpdate("alter table '"+b+"' add column '"+type+"_lasttotal' NUMERIC");
                        statement.executeUpdate("alter table '"+b+"' add column '"+type+"_timestamp' NUMERIC");
                    }
                }

                statement.executeUpdate("PRAGMA user_version = 2;");
                version = 2;
            }
            if(version == 2) {
                plugin.getLogger().info("Running SQLite table updater (pv"+version+")");

                for(String b : cacheInstance.getDbTableList()) {
                    statement.executeUpdate("alter table `"+b+"` add column displaynamecache TINYTEXT");
                }

                statement.executeUpdate("PRAGMA user_version = 3;");
            }
            if(version == 3 || version == 4) {
                TimedType type = TimedType.YEARLY;
                for(String b : cacheInstance.getDbTableList()) {
                    statement.executeUpdate("alter table `"+b+"` add column "+type.lowerName()+"_delta BIGINT");
                    statement.executeUpdate("alter table `"+b+"` add column "+type.lowerName()+"_lasttotal BIGINT");
                    statement.executeUpdate("alter table `"+b+"` add column "+type.lowerName()+"_timestamp BIGINT");
                }
                statement.executeUpdate("PRAGMA user_version = 5;");
                version = 5;
            }
        } catch (SQLException e) {
            if(e.getMessage().contains("duplicate column name")) {
                try(Statement statement = conn.createStatement()) {
                    statement.executeUpdate("PRAGMA user_version = 5;");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close(Connection connection) {
    }

    @Override
    protected LeaderboardPlugin getPlugin() {
        return plugin;
    }

    @Override
    public int getMaxConnections() {
        return 1;
    }

    @Override
    public void shutdown() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String formatStatement(String s) {
        return s.replaceAll("'", "\"");
    }

    @Override
    public String getQuotationMark() {
        return "'";
    }

    @Override
    public String getTablePrefix() {
        return "";
    }

    @Override
    public String getName() {
        return "sqlite";
    }

    @Override
    public boolean requiresClose() {
        return false;
    }

    public void newConnection() {
        shutdown();
        init(plugin, config, cacheInstance);
    }
}
