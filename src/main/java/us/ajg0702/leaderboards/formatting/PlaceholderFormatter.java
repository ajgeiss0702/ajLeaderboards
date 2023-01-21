package us.ajg0702.leaderboards.formatting;

import org.jetbrains.annotations.Nullable;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.formatting.formats.ColonTime;
import us.ajg0702.leaderboards.formatting.formats.Default;
import us.ajg0702.leaderboards.formatting.formats.Time;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderFormatter {
    private final Format defaultFormat = new Default();
    private final List<Format> formats = Arrays.asList(
            new Time(),
            new ColonTime(),

            defaultFormat
    );

    private final LeaderboardPlugin plugin;

    public PlaceholderFormatter(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    Map<String, Format> formatCache = new ConcurrentHashMap<>();

    public Format getFormatFor(@Nullable String output, String board) {
        if(output != null && output.equals("%" + board + "%")) return formatCache.getOrDefault(board, defaultFormat);
        if(output == null) {
            Format possibleMatch = formatCache.get(board);
            if(possibleMatch == null) {
                for (Format format : formats) {
                    if(format.matches(null, board)) {
                        possibleMatch = format;
                    }
                }
            }
            if(possibleMatch == null) return defaultFormat;
            return possibleMatch;
        } else {
            return formatCache.computeIfAbsent(board, b -> {
                for (Format format : formats) {
                    if(format.matches(output, board)) return format;
                }
                return defaultFormat;
            });
        }
    }

    public double toDouble(String input, String board) throws NumberFormatException {
        return getFormatFor(input, board).toDouble(input);
    }
    public String toFormat(double input, String board) {
        return getFormatFor(null, board).toFormat(input);
    }
}
