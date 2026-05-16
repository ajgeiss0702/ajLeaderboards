package us.ajg0702.leaderboards.cache;

import com.google.common.collect.ImmutableMap;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.cache.methods.H2Method;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;

@SuppressWarnings("FieldCanBeLocal")
public class ExtraManager {
    private final LeaderboardPlugin plugin;
    private final Cache cache;

    private final Map<String, String> CREATE_TABLE = ImmutableMap.of(
            "sqlite", "create table if not exists '%s' (id TEXT, placeholder VARCHAR(255), value VARCHAR(2048))",
            "h2", "create table if not exists '%s' ('id' VARCHAR(36), 'placeholder' VARCHAR(255), 'value' VARCHAR(2048))",
            "mysql", "create table if not exists '%s' ('id' VARCHAR(36), 'placeholder' VARCHAR(255), 'value' VARCHAR(2048))"
    );
    private final String QUERY_IDVALUE = "select id,'value' from '%s' where id=? and 'placeholder'=?";
    private final String INSERT_PLAYER = "insert into '%s' ('id', 'placeholder', 'value') values (?, ?, ?)";
    private final String UPDATE_PLAYER = "update '%s' set 'value'=? where id=? and 'placeholder'=?";
    private final String CREATE_LOOKUP_INDEX = "create unique index '%s_lookup' on '%s' ('id', 'placeholder')";

    private final CacheMethod method;
    private final String tableName;

    public ExtraManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.cache = plugin.getCache();

        if(cache == null) {
            throw new IllegalStateException("Cache not found. Has it been loaded?");
        }
        this.method = cache.getMethod();
        tableName = cache.getTablePrefix()+"extras";

        try (Connection conn = method.getConnection();
             PreparedStatement ps = conn.prepareStatement(method.formatStatement(String.format(
                     Objects.requireNonNull(CREATE_TABLE.get(method.getName())),
                     tableName
             )))) {
            ps.executeUpdate();
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create storage for Extras:", e);
        }

        try (Connection conn = method.getConnection();
             PreparedStatement ps = conn.prepareStatement(method.formatStatement(String.format(
                     CREATE_LOOKUP_INDEX,
                     tableName,
                     tableName
             )))) {
            ps.executeUpdate();
        } catch(SQLException e) {
            String message = e.getMessage();
            if(message != null && (message.contains("already exists") || message.contains("Duplicate key") || e.getErrorCode() == 1061)) {
            } else {
                plugin.getLogger().log(Level.WARNING, "Failed to create lookup index for Extras:", e);
            }
        }
    }

    public String getExtra(UUID id, String placeholder) {
        try (Connection conn = method.getConnection();
             PreparedStatement ps = conn.prepareStatement(method.formatStatement(String.format(
                     Objects.requireNonNull(QUERY_IDVALUE),
                     tableName
             )))) {

            ps.setString(1, id.toString());
            ps.setString(2, placeholder);

            try (ResultSet rs = ps.executeQuery()) {
                if(method instanceof MysqlMethod || method instanceof H2Method) {
                    rs.next();
                }

                String value = null;
                try {
                    value = rs.getString(2);
                } catch(SQLException e) {
                    String message = e.getMessage();
                    if(!(message != null && (
                            message.contains("ResultSet closed") ||
                                    message.contains("empty result set") ||
                                    message.contains("[2000-")
                    ))) {
                        throw e;
                    }
                }

                return value;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "An error occurred while fetching an extra:", e);
            return StatEntry.AN_ERROR_OCCURRED;
        }
    }

    public List<String> getExtras() {
        List<String> extras = new ArrayList<>();
        for (String extra : plugin.getAConfig().getStringList("extras")) {
            extra = extra.replaceAll(Matcher.quoteReplacement("%"), "");
            extras.add(extra);
        }
        return extras;
    }

    public boolean isExtra(String extra) {
        return getExtras().contains(extra);
    }

    public void setExtra(UUID id, String placeholder, String value) {
        if(plugin.isShuttingDown()) return;
        try (Connection conn = method.getConnection();
             PreparedStatement statement = conn.prepareStatement(String.format(
                     method.formatStatement(UPDATE_PLAYER),
                     tableName
             ))) {
            statement.setString(1, value);
            statement.setString(2, id.toString());
            statement.setString(3, placeholder);

            int rowsChanged = statement.executeUpdate();
            if(rowsChanged == 0) {
                try (PreparedStatement insertStmt = conn.prepareStatement(String.format(
                        method.formatStatement(INSERT_PLAYER),
                        tableName
                ))) {

                    insertStmt.setString(1, id.toString());
                    insertStmt.setString(2, placeholder);
                    insertStmt.setString(3, value);

                    insertStmt.executeUpdate();
                }
            }
        } catch(SQLException e) {
            plugin.getLogger().log(Level.WARNING, "An error occurred while inserting an extra:", e);
        }
    }

}
