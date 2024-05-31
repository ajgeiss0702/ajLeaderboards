package us.ajg0702.leaderboards.cache.methods;

import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.SQLCacheMethod;
import us.ajg0702.leaderboards.utils.UnClosableConnection;
import us.ajg0702.utils.common.ConfigFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class H2Method extends SQLCacheMethod {
    private Connection conn;
    private LeaderboardPlugin plugin;
    private ConfigFile config;
    private Cache cacheInstance;

    public H2Method(ConfigFile storageConfig) {
        super(storageConfig);
    }


    @Override
    public Connection getConnection() {
        try {
            if (conn.isClosed()) {
                plugin.getLogger().warning("H2 connection is dead, making a new one");
                init(plugin, config, cacheInstance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new UnClosableConnection(conn);
    }

    @Override
    public void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance) {
        this.plugin = plugin;
        this.config = config;
        this.cacheInstance = cacheInstance;

        // fix h2 error messages not being found (due to relocation)
        try {
            Field field = DbException.class.getDeclaredField("MESSAGES");
            field.setAccessible(true);
            ((Properties) (field.get(new Properties())))
                    .load(getClass().getResourceAsStream("/h2_messages.prop"));
        } catch (IllegalAccessException | NoSuchFieldException | IOException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to set h2 messages file! Error messages from h2 might not be very useful!", e);
        }

        File file = new File(plugin.getDataFolder(), "cache.trace.db");
        if(file.exists()) {
            plugin.getLogger().info("Deleting junk trace file");
            try {
                if(!file.delete()) {
                    plugin.getLogger().warning("Failed to delete junk trace file!");
                }
            } catch(SecurityException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete junk trace file: ", e);
            }
        }

        String url = "jdbc:h2:"+plugin.getDataFolder().getAbsolutePath()+File.separator+"cache;DATABASE_TO_UPPER=false;TRACE_LEVEL_FILE=0";
        try {
            //conn = DriverManager.getConnection(url);
            conn = new JdbcConnection(url, new Properties(), null, null, false);
        } catch (SQLException e) {
            plugin.getLogger().severe("Unnable to create cache file! The plugin will not work correctly!");
            e.printStackTrace();
            return;
        }
        List<String> tables = cacheInstance.getDbTableList();

        try(Statement statement = conn.createStatement()) {
            //ResultSet rs = conn.getMetaData().getTables(null, null, "", null);
            for(String tableName : tables) {
                int version;
                if (!tableName.startsWith(getTablePrefix())) continue;
                try {
                    ResultSet rs = conn.createStatement().executeQuery("SELECT TABLE_NAME,COLUMN_NAME,REMARKS\n" +
                            " FROM INFORMATION_SCHEMA.COLUMNS where TABLE_NAME='"+tableName+"'");
                    rs.next();
                    version = Integer.parseInt(rs.getString("REMARKS"));
                    rs.close();
                } catch(NumberFormatException e) {
                    version = 0;
                } catch(SQLException e) {
                    if(e.getMessage().contains("Column 'COMMENT' not found")) {
                        version = 0;
                    } else {
                        throw e;
                    }
                }
                Debug.info("Table version for "+tableName+" is: "+version);

                if(version == 0 || version == 1) {
                    TimedType type = TimedType.YEARLY;
                    try {
                        statement.executeUpdate("alter table \""+tableName+"\" add column "+type.lowerName()+"_delta BIGINT");
                        statement.executeUpdate("alter table \""+tableName+"\" add column "+type.lowerName()+"_lasttotal BIGINT");
                        statement.executeUpdate("alter table \""+tableName+"\" add column "+type.lowerName()+"_timestamp BIGINT");
                    } catch(SQLException e) {
                        if(e.getMessage().contains("42121")) {
//                            plugin.getLogger().info("The columns already exist for "+tableName+". Canceling updater and bumping DB version.");
                            try {
                                //conn.createStatement().executeUpdate("UPDATE INFORMATION_SCHEMA.COLUMNS where TABLE_NAME=\""+tableName+"\" SET REMARKS = '1';");
                                conn.createStatement().executeUpdate("COMMENT ON TABLE \""+tableName+"\" IS '2';");
                            } catch (SQLException er) {
                                er.printStackTrace();
                                throw e;
                            }
                        } else {
                            throw e;
                        }
                    }
                    statement.executeUpdate("COMMENT ON TABLE \"" + tableName + "\" IS '2';");
                    version = 2;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        return "`";
    }

    @Override
    public String getTablePrefix() {
        return "";
    }

    @Override
    public String getName() {
        return "h2";
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
