package us.ajg0702.leaderboards.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class ResetSaver {

    private final LeaderboardPlugin plugin;

    public ResetSaver(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeDateFormat = new SimpleDateFormat("yyyy-MM-dd_'h'HH'm'mm");

    private final List<String> illegalFileNameChars = Arrays.asList(":", ",", File.separator);

    public void save(String board, TimedType type) {

        int count = plugin.getAConfig().getInt("reset-save-positions");
        if(count <= 0) return;

        List<StatEntry> entries = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            entries.add(plugin.getCache().getStat(i, board, type));
        }

        JsonObject obj = new JsonObject();

        JsonArray jsonArray = new JsonArray();
        for (StatEntry entry : entries) {
            jsonArray.add(entry.toJsonObject());
        }

        obj.add("entries", jsonArray);

        String date = null;
        switch(type) {

            case ALLTIME:
            case HOURLY:
            case DAILY:
                date = timeDateFormat.format(new Date());
                break;
            case WEEKLY:
            case MONTHLY:
            case YEARLY:
                date = dateFormat.format(new Date());
                break;
        }

        // statistic_time_played_monthly_2023-11-01.json
        File folder = new File(
                plugin.getDataFolder().getAbsolutePath() + File.separator + "past-resets" +
                        File.separator + type.lowerName() + File.separator
        );
        File file = new File(folder,
                        board.replaceAll(
                                "(" + illegalFileNameChars.stream().map(Matcher::quoteReplacement).collect(Collectors.joining("|")) + ")",
                                "_"
                        ) +
                        "_" + type.lowerName() + "_" + date + ".json"
                );
        if(folder.mkdirs()) {
            Debug.info("past-resets/" + type.lowerName() + " folder created");
        }

        obj.add("meta",
                new EasyJsonObject()
                        .add("time", System.currentTimeMillis())
                        .add("type", type.toString())
                        .add("board", board)
                        .getHandle()
                );

        try {
            Writer writer = new FileWriter(file);
            new Gson().toJson(obj, writer);
            writer.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while writing reset save to '" + file.getPath() +"':", e);
        }
    }
}
