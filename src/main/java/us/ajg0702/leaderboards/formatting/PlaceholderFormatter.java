package us.ajg0702.leaderboards.formatting;

import org.jetbrains.annotations.Nullable;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.formatting.formats.Default;
import us.ajg0702.leaderboards.formatting.formats.Time;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PlaceholderFormatter {
    private final Format defaultFormat = new Default();
    private final List<Format> formats = Arrays.asList(
            new Time(),

            defaultFormat
    );

    private final LeaderboardPlugin plugin;

    public PlaceholderFormatter(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    HashMap<String, Format> formatCache = new HashMap<>();

    public Format getFormatFor(@Nullable String output, String board) {
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
