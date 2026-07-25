package us.ajg0702.leaderboards.cache.methods;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusHistogramMetricsTrackerFactory;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

public class MysqlMethod implements CacheMethod {
    @Override
    public Connection getConnection() throws SQLException {
        if(ds == null) {
            throw new SQLException("DataSource not initialized - HikariCP connection pool unavailable");
        }
        return ds.getConnection();
    }

    private final HikariConfig hikariConfig = new HikariConfig();
    private HikariDataSource ds;


    @Override
    public void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance) {
        String ip = config.getString("ip");
        if (ip == null) throw new IllegalArgumentException("Missing required config: mysql.ip");
        String username = config.getString("username");
        if (username == null) throw new IllegalArgumentException("Missing required config: mysql.username");
        String password = config.getString("password");
        if (password == null) throw new IllegalArgumentException("Missing required config: mysql.password");
        String database = config.getString("database");
        if (database == null) throw new IllegalArgumentException("Missing required config: mysql.database");
        boolean useSSL = config.getBoolean("useSSL");
        boolean allowPublicKeyRetrieval = config.getBoolean("allowPublicKeyRetrieval");
        int minCount = config.getInt("minConnections");
        int maxCount = config.getInt("maxConnections");
        String charEncoding = config.getString("characterEncoding");
        if (charEncoding == null) throw new IllegalArgumentException("Missing required config: mysql.characterEncoding");

        String url = "jdbc:mysql://"+ip+"/"+database+"?useSSL="+useSSL+"&useUnicode=true&character_set_server="+charEncoding+"&allowPublicKeyRetrieval="+allowPublicKeyRetrieval+"&useInformationSchema=true";
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxCount);
        hikariConfig.setMinimumIdle(minCount);
        hikariConfig.setMaxLifetime(config.getInt("maxLifetime"));

        //hikariConfig.setRegisterMbeans(true);
        //hikariConfig.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());

        ds = new HikariDataSource(hikariConfig);
        ds.setLeakDetectionThreshold(60 * 1000);

        List<String> tables = cacheInstance.getDbTableList();

        try(Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
            for(String tableName : tables) {
                int version;
                if(!tableName.startsWith(cacheInstance.getTablePrefix())) continue;
                try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("show table status where Name='"+tableName+"'")) {
                    if(rs.next()) {
                        version = Integer.parseInt(rs.getString("COMMENT"));
                    } else {
                        version = 0;
                    }
                } catch(NumberFormatException e) {
                    version = 0;
                } catch(SQLException e) {
                    String message = e.getMessage();
                    if(message != null && message.contains("Column 'COMMENT' not found")) {
                        version = 0;
                    } else {
                        throw e;
                    }
                }
                Debug.info("Table version for "+tableName+" is: "+version);

                if(version == 0) {
                    plugin.getLogger().info("Running MySQL table updater for table "+tableName+" (pv"+version+")");

                    for(TimedType typeEnum : TimedType.values()) {
                        if(typeEnum == TimedType.ALLTIME) continue;
                        String type = typeEnum.name().toLowerCase(Locale.ROOT);
                        try {
                            statement.executeUpdate("alter table `"+tableName+"` add column "+type+"_delta BIGINT");
                            statement.executeUpdate("alter table `"+tableName+"` add column "+type+"_lasttotal BIGINT");
                            statement.executeUpdate("alter table `"+tableName+"` add column "+type+"_timestamp BIGINT");
                        } catch(SQLException e) {
                            String message = e.getMessage();
                            if(message != null && message.contains("Duplicate")) {
                                plugin.getLogger().info("The columns already exist for "+tableName+". Canceling updater and bumping DB version.");
                                try (Statement stmt = conn.createStatement()) {
                                    stmt.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '1';");
                                } catch (SQLException er) {
                                    er.printStackTrace();
                                    throw e;
                                }
                            } else {
                                throw e;
                            }
                        }
                    }

                    statement.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '1';");
                    version = 1;
                }
                if(version == 1) {
                    plugin.getLogger().info("Running MySQL table updater for table "+tableName+" (pv"+version+")");

                    try {
                        statement.executeUpdate("alter table `"+tableName+"` add column displaynamecache TINYTEXT");
                    } catch(SQLException e) {
                        String message = e.getMessage();
                        if(message != null && message.contains("Duplicate")) {
                            plugin.getLogger().info("The columns already exist for "+tableName+". Canceling updater and bumping DB version.");
                            try (Statement stmt = conn.createStatement()) {
                                stmt.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '2';");
                            } catch (SQLException er) {
                                er.printStackTrace();
                                throw e;
                            }
                        } else {
                            throw e;
                        }
                    }
                    statement.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '2';");
                    version = 2;
                }
                if(version == 2) {
                    TimedType type = TimedType.YEARLY;
                    try {
                        statement.executeUpdate("alter table `"+tableName+"` add column "+type+"_delta BIGINT");
                        statement.executeUpdate("alter table `"+tableName+"` add column "+type+"_lasttotal BIGINT");
                        statement.executeUpdate("alter table `"+tableName+"` add column "+type+"_timestamp BIGINT");
                    } catch(SQLException e) {
                        String message = e.getMessage();
                        if(message != null && message.contains("Duplicate")) {
                            plugin.getLogger().info("The columns already exist for "+tableName+". Canceling updater and bumping DB version.");
                            try (Statement stmt = conn.createStatement()) {
                                stmt.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '3';");
                            } catch (SQLException er) {
                                er.printStackTrace();
                                throw e;
                            }
                        } else {
                            throw e;
                        }
                    }
                    statement.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '3';");
                    version = 3;
                }
                if(version == 3) {
                    for (TimedType type : TimedType.values()) {
                        if(type == TimedType.ALLTIME) continue;
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate(
                                    "create index "+type.lowerName()+"_timestamp on `"+tableName+"` ("+type.lowerName()+"_timestamp)"
                            );
                        } catch(SQLException e) {
                            String message = e.getMessage();
                            if(message == null || !message.contains("Duplicate key name")) throw e;
                        }
                    }
                    statement.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '4';");
                    version = 4;
                }
                if(version == 4) {
                    for (TimedType type : TimedType.values()) {
                        String index = type == TimedType.ALLTIME ? "value" : type.lowerName()+"_delta";
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate(
                                    "create index " + index + " on `"+tableName+"` (" + index + ")"
                            );
                        } catch(SQLException e) {
                            String message = e.getMessage();
                            if((message == null || !message.contains("Duplicate key name")) && e.getErrorCode() != 1061) {
                                throw e;
                            }
                        }
                    }
                    statement.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '5';");
                    version = 5;
                }
                if(version == 5) {
                    plugin.getLogger().info("Running MySQL table updater for table "+tableName+" (pv"+version+")");
                    try {
                        statement.executeUpdate("alter table `"+tableName+"` add column last_updated BIGINT DEFAULT 0");
                    } catch(SQLException e) {
                        String message = e.getMessage();
                        if(message != null && message.contains("Duplicate")) {
                            plugin.getLogger().info("The column already exists for "+tableName+". Canceling updater and bumping DB version.");
                            try (Statement stmt = conn.createStatement()) {
                                stmt.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '6';");
                            } catch (SQLException er) {
                                er.printStackTrace();
                                throw e;
                            }
                        } else {
                            throw e;
                        }
                    }
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("create index last_updated_idx on `"+tableName+"` (last_updated)");
                    } catch(SQLException e) {
                        String message = e.getMessage();
                        if((message == null || !message.contains("Duplicate key name")) && e.getErrorCode() != 1061) throw e;
                    }
                    statement.executeUpdate("ALTER TABLE `"+tableName+"` COMMENT = '6';");
                    version = 6;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public int getMaxConnections() {
        return ds.getMaximumPoolSize();
    }

    @Override
    public void shutdown() {
        ds.close();
    }

    @Override
    public String formatStatement(String s) {
        return s.replaceAll("'", "`");
    }

    @Override
    public String getName() {
        return "mysql";
    }

    @Override
    public boolean requiresClose() {
        return true;
    }
}
