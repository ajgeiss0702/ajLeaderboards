package us.ajg0702.leaderboards.cache;

import com.google.common.collect.ImmutableMap;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.keys.BoardType;
import us.ajg0702.leaderboards.cache.helpers.DbRow;
import us.ajg0702.leaderboards.cache.methods.H2Method;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;
import us.ajg0702.leaderboards.cache.methods.SqliteMethod;
import us.ajg0702.leaderboards.utils.Partition;
import us.ajg0702.utils.common.ConfigFile;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public abstract class SQLCacheMethod implements CacheMethod {
    final ConfigFile storageConfig;
    private final String SELECT_POSITION = "select 'id','value','namecache','prefixcache','suffixcache','displaynamecache'," + deltaBuilder() + " from '%s' order by '%s' %s, namecache desc limit 1 offset %d";
    private final String GET_POSITION = "/*%s*/with N as (select *,ROW_NUMBER() OVER (order by '%s' %s) as position from '%s') select 'id','value','namecache','prefixcache','suffixcache','displaynamecache',position," + deltaBuilder() + " from N where 'id'=?";
    private final Map<String, String> CREATE_TABLE = ImmutableMap.of(
            "sqlite", "create table if not exists '%s' (id TEXT PRIMARY KEY, value DECIMAL(20, 2)" + columnBuilder("NUMERIC") + ", namecache TEXT, prefixcache TEXT, suffixcache TEXT, displaynamecache TEXT)",
            "h2", "create table if not exists '%s' ('id' VARCHAR(36) PRIMARY KEY, 'value' DECIMAL(20, 2)" + columnBuilder("BIGINT") + ", 'namecache' VARCHAR(16), 'prefixcache' VARCHAR(255), 'suffixcache' VARCHAR(255), 'displaynamecache' VARCHAR(512))",
            "mysql", "create table if not exists '%s' ('id' VARCHAR(36) PRIMARY KEY, 'value' DECIMAL(20, 2)" + columnBuilder("BIGINT") + ", 'namecache' VARCHAR(16), 'prefixcache' TINYTEXT, 'suffixcache' TINYTEXT, 'displaynamecache' VARCHAR(512))"
    );
    private final Map<String, String> CREATE_EXTRA_TABLE = ImmutableMap.of(
            "sqlite", "create table if not exists '%s' (id TEXT, placeholder VARCHAR(255), value VARCHAR(255))",
            "h2", "create table if not exists '%s' ('id' VARCHAR(36), 'placeholder' VARCHAR(255), 'value' VARCHAR(255))",
            "mysql", "create table if not exists '%s' ('id' VARCHAR(36), 'placeholder' VARCHAR(255), 'value' VARCHAR(255))"
    );
    private final String REMOVE_PLAYER = "delete from '%s' where 'namecache'=?";
    private final Map<String, String> LIST_TABLES = ImmutableMap.of(
            "sqlite", "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';"
    );
    private final String DROP_TABLE = "drop table '%s';";
    private final String INSERT_PLAYER = "insert into '%s' ('id', 'value', 'namecache', 'prefixcache', 'suffixcache', 'displaynamecache'" + tableBuilder() + ") values (?, ?, ?, ?, ?, ?" + qBuilder() + ")";
    private final String UPDATE_PLAYER = "update '%s' set 'value'=?, 'namecache'=?, 'prefixcache'=?, 'suffixcache'=?, 'displaynamecache'=?" + updateBuilder() + " where id=?";
    private final String INSERT_PLAYER_EXTRA = "insert into '%s' ('id', 'placeholder', 'value') values (?, ?, ?)";
    private final String UPDATE_PLAYER_EXTRA = "update '%s' set 'value'=? where id=? and 'placeholder'=?";
    private final String QUERY_IDVALUE_EXTRA = "select id,'value' from '%s' where id=? and 'placeholder'=?";
    private final String QUERY_LASTTOTAL = "select '%s' from '%s' where id=?";
    private final String QUERY_LASTRESET = "select '%s' from '%s' limit 1";
    private final String QUERY_IDVALUE = "select id,'value' from '%s'";
    private final String UPDATE_RESET = "update '%s' set '%s'=?, '%s'=?, '%s'=? where id=?";
    private final String QUERY_ALL = "select * from '%s'";
    private final String CREATE_TIMESTAMP_INDEX = "create index %s_timestamp on '%s' (%s_timestamp)";
    private final Map<String, Integer> sortByIndexes = new ConcurrentHashMap<>();
    Map<String, Integer> dataSortByIndexes = new ConcurrentHashMap<>();
    List<String> nonExistantBoards = new ArrayList<>();

    protected SQLCacheMethod(ConfigFile storageConfig) {
        this.storageConfig = storageConfig;
    }

    abstract public Connection getConnection() throws SQLException;

    abstract public int getMaxConnections();

    abstract public String formatStatement(String s);

    abstract public String getQuotationMark();

    abstract public String getTablePrefix();

    abstract public void close(Connection connection) throws SQLException;

    protected abstract LeaderboardPlugin getPlugin();

    @Override
    public StatEntry getStatEntry(int position, String board, TimedType type) throws SQLException {
        Connection conn = this.getConnection();
        String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
        PreparedStatement ps = conn.prepareStatement(String.format(
                this.formatStatement(SELECT_POSITION),
                getTablePrefix() + board,
                sortBy,
                isReverse(board) ? "asc" : "desc",
                position - 1
        ));

        ResultSet r = ps.executeQuery();

        StatEntry se = processData(r, sortBy, position, board, type);
        ps.close();
        this.close(conn);
        return se;
    }

    @Override
    @Nullable
    public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
        StatEntry r = null;
        try {
            Connection conn = this.getConnection();
            String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
            PreparedStatement ps = conn.prepareStatement(String.format(
                    this.formatStatement(GET_POSITION),
                    board,
                    sortBy,
                    isReverse(board) ? "asc" : "desc",
                    getTablePrefix() + board
            ));

            ps.setString(1, player.getUniqueId().toString());

            ResultSet rs = ps.executeQuery();

            rs.next();

            String uuidraw = null;
            double value = -1;
            String name = "-Unknown-";
            String displayName = name;
            String prefix = "";
            String suffix = "";
            int position = -1;
            try {
                uuidraw = rs.getString(1);
                name = rs.getString(3);
                prefix = rs.getString(4);
                suffix = rs.getString(5);
                displayName = rs.getString(6);
                position = rs.getInt(7);
                value = rs.getDouble(sortByIndexes.computeIfAbsent(sortBy,
                        k -> {
                            try {
                                Debug.info("Calculating (statentry) column for " + sortBy);
                                return rs.findColumn(sortBy);
                            } catch (SQLException e) {
                                getPlugin().getLogger().log(Level.SEVERE, "Error while finding a column for " + sortBy, e);
                                return -1;
                            }
                        }
                ));
            } catch (SQLException e) {
                if (
                        !e.getMessage().contains("ResultSet closed") &&
                                !e.getMessage().contains("empty result set") &&
                                !e.getMessage().contains("[2000-")
                ) {
                    throw e;
                }
            }
            if (uuidraw != null) {
                r = new StatEntry(position, board, prefix, name, displayName, UUID.fromString(uuidraw), suffix, value, type);
            }
            rs.close();
            ps.close();
            this.close(conn);
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "Unable to get position/value of player:", e);
            return StatEntry.error(-1, board, type);
        }
        return r;
    }

    @Override
    public int getBoardSize(String board) {
        Connection connection = null;
        ResultSet rs = null;

        int size;

        try {
            connection = this.getConnection();

            PreparedStatement ps = connection.prepareStatement(String.format(
                    this.formatStatement("select COUNT(1) from '%s'"),
                    getTablePrefix() + board
            ));

            rs = ps.executeQuery();

            rs.next();

            size = rs.getInt(1);

        } catch (SQLException e) {
            if (
                    !e.getMessage().contains("ResultSet closed") &&
                            !e.getMessage().contains("empty result set") &&
                            !e.getMessage().contains("[2000-")
            ) {
                getPlugin().getLogger().log(Level.WARNING, "Unable to get size of board:", e);
                return -1;
            } else {
                return 0;
            }
        } finally {
            try {
                if (connection != null) this.close(connection);
                if (rs != null) rs.close();
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.WARNING, "Error while closing resources from board size fetch:", e);
            }

        }

        return size;
    }

    @Override
    public double getTotal(String board, TimedType type) {
        if(!getPlugin().getTopManager().boardExists(board)) {
            if(!nonExistantBoards.contains(board)) {
                nonExistantBoards.add(board);
            }
            return -3;
        }

        Connection connection = null;
        ResultSet rs = null;

        int size;

        try {
            connection = this.getConnection();

            PreparedStatement ps = connection.prepareStatement(String.format(
                    this.formatStatement("select SUM(%s) from '%s'"),
                    type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta",
                    getTablePrefix()+board
            ));

            rs = ps.executeQuery();

            rs.next();

            size = rs.getInt(1);

        } catch (SQLException e) {
            if(
                    !e.getMessage().contains("ResultSet closed") &&
                            !e.getMessage().contains("empty result set") &&
                            !e.getMessage().contains("[2000-")
            ) {
                getPlugin().getLogger().log(Level.WARNING, "Unable to get size of board:", e);
                return -1;
            } else {
                return 0;
            }
        } finally {
            try {
                if(connection != null) this.close(connection);
                if(rs != null) rs.close();
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.WARNING, "Error while closing resources from board size fetch:", e);
            }
        }

        return size;
    }

    @Override
    public boolean createBoard(String name) {
        try {
            Connection conn = this.getConnection();
            PreparedStatement ps = conn.prepareStatement(this.formatStatement(String.format(
                    CREATE_TABLE.get(this.getName()),
                    getTablePrefix() + name
            )));

            ps.executeUpdate();

            ps.close();

            for (TimedType type : TimedType.values()) {
                if (type == TimedType.ALLTIME) continue;

                try {
                    ps = conn.prepareStatement(this.formatStatement(String.format(
                            CREATE_TIMESTAMP_INDEX,
                            type.lowerName(),
                            getTablePrefix() + name,
                            type.lowerName()
                    )));
                    ps.executeUpdate();
                } catch (SQLException e) {
                    if (!e.getMessage().contains("already exists") && !e.getMessage().contains("Duplicate key"))
                        throw e;
                }
                ps.close();
            }

            this.close(conn);
            getPlugin().getTopManager().fetchBoards();
            getPlugin().getContextLoader().calculatePotentialContexts();
            nonExistantBoards.remove(name);
            if (!getPlugin().getTopManager().boardExists(name)) {
                getPlugin().getLogger().warning("Failed to create board: It wasnt created, but there was no error!");
                return false;
            }
            return true;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "Unable to create board:", e);
            if (e.getCause() != null) {
                getPlugin().getLogger().log(Level.WARNING, "Cause:", e);
            }
            return false;
        }
    }

    @Override
    public boolean removePlayer(String board, String playerName) {
        try {
            Connection conn = this.getConnection();
            PreparedStatement ps = conn.prepareStatement(String.format(
                    this.formatStatement(REMOVE_PLAYER),
                    getTablePrefix() + board
            ));

            ps.setString(1, playerName);

            ps.executeUpdate();

            ps.close();
            this.close(conn);
            return true;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "Unable to remove player from board:", e);
            return false;
        }
    }

    @Override
    public List<String> getBoards() {
        List<String> o = new ArrayList<>();

        for (String table : getDbTableList()) {
            if (table.indexOf(getTablePrefix()) != 0) continue;
            String name = table.substring(getTablePrefix().length());
            if (name.equals("extras")) continue;
            o.add(name);
        }

        return o;
    }

    @Override
    public List<String> getDbTableList() {
        List<String> b = new ArrayList<>();
        try {

            ResultSet r;
            Connection conn = this.getConnection();
            Statement statement = conn.createStatement();
            r = statement.executeQuery(
                    this.formatStatement(
                            LIST_TABLES.getOrDefault(this.getName(), "show tables;")
                    )
            );
            while (r.next()) {
                String e = r.getString(1);
                if (e.indexOf(getTablePrefix()) != 0) continue;
                String name = e.substring(getTablePrefix().length());
                if (name.equals("extras")) continue;
                b.add(e);
            }

            statement.close();
            r.close();
            this.close(conn);
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "Unable to get list of tables:", e);
        }
        return b;
    }

    @Override
    public boolean removeBoard(String board) {
        if (!getPlugin().getTopManager().boardExists(board)) {
            getPlugin().getLogger().warning("Attempted to remove board that doesnt exist!");
            return false;
        }
        try {
            if (this instanceof SqliteMethod) {
                ((SqliteMethod) this).newConnection();
            }
            Connection conn = this.getConnection();
            PreparedStatement ps = conn.prepareStatement(String.format(
                    this.formatStatement(DROP_TABLE),
                    getTablePrefix() + board
            ));
            ps.executeUpdate();

            ps.close();
            this.close(conn);
            getPlugin().getTopManager().fetchBoards();
            getPlugin().getContextLoader().calculatePotentialContexts();
            if (getPlugin().getTopManager().boardExists(board)) {
                getPlugin().getLogger().warning("Attempted to remove a board, but it didnt get removed!");
                return false;
            }
            return true;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "An error occurred while trying to remove a board:", e);
            return false;
        }
    }

    @Override
    public void upsertPlayer(String board, OfflinePlayer player, double output, String prefix, String suffix, String displayName) {
        Map<TimedType, Double> lastTotals = new HashMap<>();
        for (TimedType type : TimedType.values()) {
            if (type == TimedType.ALLTIME) continue;
            lastTotals.put(type, getLastTotal(board, player, type));
        }
        try (Connection conn = this.getConnection()) {
            try {
                PreparedStatement statement = conn.prepareStatement(String.format(
                        this.formatStatement(INSERT_PLAYER),
                        getTablePrefix() + board
                ));
                statement.setString(1, player.getUniqueId().toString());
                statement.setDouble(2, output);
                statement.setString(3, player.getName());
                statement.setString(4, prefix);
                statement.setString(5, suffix);
                statement.setString(6, displayName);
                int i = 6;
                for (TimedType type : TimedType.values()) {
                    if (type == TimedType.ALLTIME) continue;
                    long lastReset = getPlugin().getTopManager().getLastReset(board, type) * 1000;
                    if (getPlugin().isShuttingDown()) {
                        this.close(conn);
                    }
                    statement.setDouble(++i, 0);
                    statement.setDouble(++i, output);
                    statement.setLong(++i, lastReset == 0 ? System.currentTimeMillis() : lastReset);
                }

                statement.executeUpdate();
                statement.close();
                this.close(conn);
            } catch (SQLException e) {
                try (PreparedStatement statement = conn.prepareStatement(String.format(
                        this.formatStatement(UPDATE_PLAYER),
                        getTablePrefix() + board
                ))) {
                    statement.setDouble(1, output);
                    statement.setString(2, player.getName());
                    statement.setString(3, prefix);
                    statement.setString(4, suffix);
                    statement.setString(5, displayName);
                    Map<TimedType, Double> timedTypeValues = new HashMap<>();
                    timedTypeValues.put(TimedType.ALLTIME, output);
                    int i = 6;
                    for (TimedType type : TimedType.values()) {
                        if (type == TimedType.ALLTIME) continue;
                        double timedOut = output - lastTotals.get(type);
                        timedTypeValues.put(type, timedOut);
                        statement.setDouble(i++, timedOut);
                    }
                    for (Map.Entry<TimedType, Double> timedTypeDoubleEntry : timedTypeValues.entrySet()) {
                        TimedType type = timedTypeDoubleEntry.getKey();
                        double timedOut = timedTypeDoubleEntry.getValue();

                        StatEntry statEntry = getPlugin().getTopManager().getCachedStatEntry(player, board, type, false);
                        if (statEntry != null && player.getUniqueId().equals(statEntry.getPlayerID())) {
                            statEntry.changeScore(timedOut, prefix, suffix);
                        }

                        Integer position = getPlugin().getTopManager()
                                .positionPlayerCache.getOrDefault(player.getUniqueId(), new HashMap<>())
                                .get(new BoardType(board, type));
                        if (position != null) {
                            StatEntry stat = getPlugin().getTopManager().getCachedStat(position, board, type);
                            if (stat != null && player.getUniqueId().equals(stat.getPlayerID())) {
                                stat.changeScore(timedOut, prefix, suffix);
                            }
                        }
                    }
                    statement.setString(i, player.getUniqueId().toString());
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            if (getPlugin().isShuttingDown()) return;
            getPlugin().getLogger().log(Level.WARNING, "Unable to update stat for player:", e);
        }
    }

    @Override
    public double getLastTotal(String board, OfflinePlayer player, TimedType type) {
        double last = 0;
        try {
            Connection conn = this.getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(String.format(
                        this.formatStatement(QUERY_LASTTOTAL),
                        type.lowerName() + "_lasttotal",
                        getTablePrefix() + board
                ));

                ps.setString(1, player.getUniqueId().toString());

                ResultSet rs = ps.executeQuery();

                if (this instanceof MysqlMethod || this instanceof H2Method) {
                    rs.next();
                }
                last = rs.getDouble(1);
                rs.close();
                ps.close();
                this.close(conn);
            } catch (SQLException e) {
                this.close(conn);
                String m = e.getMessage();
                if (m.contains("empty result set") || m.contains("ResultSet closed") || m.contains("[2000-"))
                    return last;
                getPlugin().getLogger().log(Level.WARNING, "Unable to get last total for " + player.getName() + " on " + type + " of " + board, e);
            }
        } catch (SQLException ignored) {
        }

        return last;
    }

    @Override
    public long getLastReset(String board, TimedType type) {
        long last = 0;
        try {
            Connection conn = this.getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(String.format(
                        this.formatStatement(QUERY_LASTRESET),
                        type.lowerName() + "_timestamp",
                        getTablePrefix() + board
                ));

                ResultSet rs = ps.executeQuery();
                if (this instanceof MysqlMethod || this instanceof H2Method) {
                    rs.next();
                }
                last = rs.getLong(1);
                ps.close();
                this.close(conn);
            } catch (SQLException e) {
                this.close(conn);
                String m = e.getMessage();
                if (m.contains("empty result set") || m.contains("ResultSet closed") || m.contains("[2000-"))
                    return last;
                getPlugin().getLogger().log(Level.WARNING, "Unable to get last reset for " + type + " of " + board, e);
            }
        } catch (SQLException ignored) {
        }

        return last;
    }

    @Override
    public void resetBoard(String board, TimedType type, long newTime) {
        String t = type.lowerName();
        try {
            Connection conn = this.getConnection();
            PreparedStatement ps = conn.prepareStatement(String.format(
                    this.formatStatement(QUERY_IDVALUE),
                    getTablePrefix() + board
            ));

            ResultSet rs = ps.executeQuery();
            Map<String, Double> uuids = new HashMap<>();
            while (rs.next()) {
                uuids.put(rs.getString(1), rs.getDouble(2));
            }
            rs.close();
            ps.close();
            this.close(conn);
            Partition<String> partition = Partition.ofSize(new ArrayList<>(uuids.keySet()), Math.max(uuids.size() / (int) Math.ceil(this.getMaxConnections() / 2D), 1));
            Debug.info("Partition length: " + partition.size() + " uuids size: " + uuids.size() + " partition chunk size: " + partition.getChunkSize());
            for (List<String> uuidPartition : partition) {
                if (getPlugin().isShuttingDown()) {
                    this.close(conn);
                    return;
                }
                try {
                    Connection con = this.getConnection();
                    for (String idRaw : uuidPartition) {
                        if (getPlugin().isShuttingDown()) {
                            this.close(con);
                            return;
                        }
                        PreparedStatement p = con.prepareStatement(String.format(
                                this.formatStatement(UPDATE_RESET),
                                getTablePrefix() + board,
                                t + "_lasttotal",
                                t + "_delta",
                                t + "_timestamp"
                        ));
                        p.setDouble(1, uuids.get(idRaw));
                        p.setDouble(2, 0);
                        p.setLong(3, newTime);
                        p.setString(4, idRaw);
                        p.executeUpdate();
                        p.close();
                    }
                    this.close(con);
                } catch (SQLException e) {
                    getPlugin().getLogger().log(Level.WARNING, "An error occurred while resetting " + type + " of " + board + ":", e);
                }
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "An error occurred while resetting " + type + " of " + board + ":", e);
        }
    }

    @Override
    public void insertRows(String board, List<DbRow> rows) throws SQLException {
        Connection conn = this.getConnection();
        for (DbRow row : rows) {
            PreparedStatement statement = conn.prepareStatement(String.format(
                    this.formatStatement(INSERT_PLAYER),
                    getTablePrefix() + board
            ));
            statement.setString(1, row.getId().toString());
            statement.setDouble(2, row.getValue());
            statement.setString(3, row.getNamecache());
            statement.setString(4, row.getPrefixcache());
            statement.setString(5, row.getSuffixcache());
            statement.setString(6, row.getDisplaynamecache());
            int i = 6;
            for (TimedType type : TimedType.values()) {
                if (type == TimedType.ALLTIME) continue;
                if (getPlugin().isShuttingDown()) {
                    this.close(conn);
                }
                statement.setDouble(++i, row.getDeltas().get(type));
                statement.setDouble(++i, row.getLastTotals().get(type));
                statement.setLong(++i, row.getTimestamps().get(type));
            }

            try {
                statement.executeUpdate();
            } catch (SQLException e) {
                if (e.getMessage().contains("23505") || e.getMessage().contains("Duplicate entry") || e.getMessage().contains("PRIMARY KEY constraint failed")) {
                    statement.close();
                    continue;
                }
                throw e;
            }
            statement.close();
        }
        this.close(conn);
    }

    @Override
    public List<DbRow> getRows(String board) throws SQLException {
        Connection conn = this.getConnection();
        PreparedStatement ps = conn.prepareStatement(String.format(
                this.formatStatement(QUERY_ALL),
                getTablePrefix() + board
        ));
        ResultSet resultSet = ps.executeQuery();

        List<DbRow> out = new ArrayList<>();

        while (resultSet.next()) {
            out.add(new DbRow(resultSet));
        }

        ps.close();
        resultSet.close();
        this.close(conn);
        return out;
    }

    @Override
    public String getExtra(UUID id, String placeholder) {
        try {
            Connection conn = this.getConnection();
            PreparedStatement ps = conn.prepareStatement(this.formatStatement(String.format(
                    Objects.requireNonNull(QUERY_IDVALUE_EXTRA),
                    getTablePrefix() + "extras"
            )));

            ps.setString(1, id.toString());
            ps.setString(2, placeholder);

            ResultSet rs = ps.executeQuery();

            if (this instanceof MysqlMethod || this instanceof H2Method) {
                rs.next();
            }

            String value = null;
            try {
                value = rs.getString(2);
            } catch (SQLException e) {
                if (
                        !e.getMessage().contains("ResultSet closed") &&
                                !e.getMessage().contains("empty result set") &&
                                !e.getMessage().contains("[2000-")
                ) {
                    throw e;
                }
            }

            rs.close();
            ps.close();
            this.close(conn);
            return value;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "An error occurred while fetching an extra:", e);
            return StatEntry.AN_ERROR_OCCURRED;
        }
    }

    @Override
    public void upsertExtra(UUID id, String placeholder, String value) {
        try {
            Connection conn = this.getConnection();

            PreparedStatement statement = conn.prepareStatement(String.format(
                    this.formatStatement(UPDATE_PLAYER_EXTRA),
                    getTablePrefix() + "extras"
            ));
            statement.setString(1, value);
            statement.setString(2, id.toString());
            statement.setString(3, placeholder);

            int rowsChanged = statement.executeUpdate();
            statement.close();
            if (rowsChanged == 0) {
                PreparedStatement insertStmt = conn.prepareStatement(String.format(
                        this.formatStatement(INSERT_PLAYER_EXTRA),
                        getTablePrefix() + "extras"
                ));

                insertStmt.setString(1, id.toString());
                insertStmt.setString(2, placeholder);
                insertStmt.setString(3, value);

                insertStmt.executeUpdate();
                insertStmt.close();
            }
            this.close(conn);
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.WARNING, "An error occurred while inserting an extra:", e);
        }
    }

    @Override
    public void createExtraTable() {
        try {
            Connection conn = this.getConnection();
            PreparedStatement ps = conn.prepareStatement(this.formatStatement(String.format(
                    Objects.requireNonNull(CREATE_EXTRA_TABLE.get(this.getName())),
                    getTablePrefix() + "extras"
            )));

            ps.executeUpdate();

            ps.close();
            this.close(conn);
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to create storage for Extras:", e);
        }
    }

    private boolean isReverse(String board) {
        return storageConfig.getStringList("reverse-sort").contains(board);
    }

    private String deltaBuilder() {
        StringBuilder deltaBuilder = new StringBuilder();
        for (TimedType t : TimedType.values()) {
            if (t == TimedType.ALLTIME) continue;
            deltaBuilder.append(getQuotationMark()).append(t.lowerName()).append("_delta").append(getQuotationMark()).append(",");
        }
        return deltaBuilder.deleteCharAt(deltaBuilder.length() - 1).toString();
    }

    private String columnBuilder(String t) {
        String q = "'";
        StringBuilder columns = new StringBuilder();
        for (TimedType type : TimedType.values()) {
            if (type == TimedType.ALLTIME) continue;
            columns
                    .append(",\n").append(q).append(type.lowerName()).append("_delta").append(q).append(" ").append(t)
                    .append(",\n").append(q).append(type.lowerName()).append("_lasttotal").append(q).append(" ").append(t)
                    .append(",\n").append(q).append(type.lowerName()).append("_timestamp").append(q).append(" ").append(t);
        }
        return columns.toString();
    }

    private String qBuilder() {
        StringBuilder addQs = new StringBuilder();
        for (TimedType type : TimedType.values()) {
            if (type == TimedType.ALLTIME) continue;
            addQs.append(", ?").append(", ?").append(", ?");
        }
        return addQs.toString();
    }

    private String tableBuilder() {
        StringBuilder addTables = new StringBuilder();
        for (TimedType type : TimedType.values()) {
            if (type == TimedType.ALLTIME) continue;
            String name = type.lowerName();
            addTables
                    .append(", ").append(name).append("_delta")
                    .append(", ").append(name).append("_lasttotal")
                    .append(", ").append(name).append("_timestamp");
        }
        return addTables.toString();
    }

    private String updateBuilder() {
        StringBuilder addUpdates = new StringBuilder();
        for (TimedType type : TimedType.values()) {
            if (type == TimedType.ALLTIME) continue;
            String name = type.lowerName();
            addUpdates
                    .append(", ").append(name).append("_delta").append("=?");
        }
        return addUpdates.toString();
    }

    private StatEntry processData(ResultSet r, String sortBy, int position, String board, TimedType type) throws SQLException {
        String uuidRaw = null;
        double value = -1;
        String name = "-Unknown-";
        String displayName = name;
        String prefix = "";
        String suffix = "";
        if (this instanceof MysqlMethod || this instanceof H2Method) {
            r.next();
        }
        try {
            uuidRaw = r.getString(1);
            name = r.getString(3);
            prefix = r.getString(4);
            suffix = r.getString(5);
            displayName = r.getString(6);
            value = r.getDouble(dataSortByIndexes.computeIfAbsent(sortBy,
                    k -> {
                        try {
                            Debug.info("Calculating (process) column for " + sortBy);
                            return r.findColumn(sortBy);
                        } catch (SQLException e) {
                            getPlugin().getLogger().log(Level.SEVERE, "Error while finding a column for " + sortBy, e);
                            return -1;
                        }
                    }
            ));
        } catch (SQLException e) {
            if (
                    !e.getMessage().contains("ResultSet closed") &&
                            !e.getMessage().contains("empty result set") &&
                            !e.getMessage().contains("[2000-")
            ) {
                throw e;
            }
        }
        if (name == null) name = "-Unknown";
        r.close();
        if (uuidRaw == null) {
            return StatEntry.noData(getPlugin(), position, board, type);
        } else {
            return new StatEntry(position, board, prefix, name, displayName, UUID.fromString(uuidRaw), suffix, value, type);
        }
    }

    protected ConfigFile getStorageConfig() {
        return storageConfig;
    }
}
