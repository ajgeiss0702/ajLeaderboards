package us.ajg0702.leaderboards.placeholders.placeholders.player;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Matcher;

public class PlayerValueRaw extends Placeholder {
    public PlayerValueRaw(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "value_(.*)_(.*)_raw";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        if(p == null) return "No player!";
        String board = matcher.group(1);
        String typeRaw = matcher.group(2).toUpperCase(Locale.ROOT);
        TimedType type = TimedType.of(typeRaw);
        if(type == null) {
            return "Invalid TimedType '" + typeRaw + "'";
        }
        StatEntry r = plugin.getTopManager().getStatEntry(p, board, type);
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(r.getScore());
    }
}
