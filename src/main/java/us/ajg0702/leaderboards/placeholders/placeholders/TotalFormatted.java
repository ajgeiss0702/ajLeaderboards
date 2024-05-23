package us.ajg0702.leaderboards.placeholders.placeholders;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.Locale;
import java.util.regex.Matcher;

import static us.ajg0702.leaderboards.boards.StatEntry.formatDouble;

public class TotalFormatted extends Placeholder {
    public TotalFormatted(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "total_(.*)_(.*)_formatted";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        String typeRaw = matcher.group(2).toUpperCase(Locale.ROOT);
        TimedType type = TimedType.of(typeRaw);
        if(type == null) {
            return "Invalid TimedType '" + typeRaw + "'";
        }
        double total = plugin.getTopManager().getTotal(board, type);
        return formatDouble(total);
    }
}
