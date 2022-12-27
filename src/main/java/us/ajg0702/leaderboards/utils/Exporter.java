package us.ajg0702.leaderboards.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.Nullable;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.helpers.DbRow;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class Exporter {
    private final LeaderboardPlugin plugin;

    public Exporter(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public JsonObject export(@Nullable Audience reportTo) {
        if(reportTo != null) reportTo.sendMessage(plugin.getMessages().getComponent("commands.export.starting"));
        plugin.getLogger().info("Starting export");

        HashMap<String, List<DbRow>> rows = new HashMap<>();
        int i = 0;
        List<String> boards = plugin.getCache().getBoards();
        for(String board : boards) {
            try {
                DbRow.clearPositionCache();
                rows.put(board, plugin.getCache().getRows(board));
                if(reportTo != null) reportTo.sendMessage(plugin.getMessages().getComponent("commands.export.progress", "DONE:"+ ++i, "TOTAL:"+boards.size()));
                plugin.getLogger().info(String.format("Export progress: %d/%d fetched", i, boards.size()));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while fetching rows from the database:", e);
                return null;
            }
        }

        JsonObject obj = new JsonObject();
        for(Map.Entry<String, List<DbRow>> entry : rows.entrySet()) {
            JsonArray elements = new JsonArray();
            entry.getValue().forEach((t) -> elements.add(t.toJsonObject()));
            obj.add(entry.getKey(), elements);
        }

        return obj;
    }
}
