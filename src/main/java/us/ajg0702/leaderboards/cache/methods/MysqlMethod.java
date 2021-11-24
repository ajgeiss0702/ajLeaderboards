package us.ajg0702.leaderboards.cache.methods;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.Connection;
import java.sql.SQLException;

public class MysqlMethod implements CacheMethod {
    @Override
    public Connection getConnection() throws SQLException {
        if(ds == null) return null;
        return ds.getConnection();
    }

    private final HikariConfig hikariConfig = new HikariConfig();
    private HikariDataSource ds;


    @Override
    public void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance) {
        String ip = config.getString("ip");
        String username = config.getString("username");
        String password = config.getString("password");
        String database = config.getString("database");
        boolean useSSL = config.getBoolean("useSSL");
        boolean allowPublicKeyRetrieval = config.getBoolean("allowPublicKeyRetrieval");
        int minCount = config.getInt("minConnections");
        int maxCount = config.getInt("maxConnections");

        String url = "jdbc:mysql://"+ip+"/"+database+"?useSSL="+useSSL+"&allowPublicKeyRetrieval="+allowPublicKeyRetrieval+"&characterEncoding=utf8";
        hikariConfig.setDriverClassName("com.mysql.jdbc.Driver");
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxCount);
        hikariConfig.setMinimumIdle(minCount);
        ds = new HikariDataSource(hikariConfig);
        ds.setLeakDetectionThreshold(60 * 1000);
    }

    @Override
    public void close(Connection connection) throws SQLException {
        connection.close();
    }
}
