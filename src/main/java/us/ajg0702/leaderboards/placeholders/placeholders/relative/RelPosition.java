package us.ajg0702.leaderboards.placeholders.placeholders.relative;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.Locale;
import java.util.regex.Matcher;

public class RelPosition extends Placeholder {
    public RelPosition(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "rel_(.*)_(.*)_([+-])([1-9][0-9]*)_position";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        String typeRaw = matcher.group(2).toUpperCase(Locale.ROOT);
        String posneg = matcher.group(3);
        int position = Integer.parseInt(matcher.group(4));
        if(posneg.equals("-")) position *= -1;
        StatEntry r = plugin.getTopManager().getRelative(p, position, board, TimedType.valueOf(typeRaw));
        String pos = StatEntry.addCommas(
                r.getPosition()
        );
        if(!r.hasPlayer() && r.getScore() == -2) {
            return plugin.getMessages().getRawString(
                    "no-data.rel.position",
                    "POSITION:"+pos
            );
        }
        return pos;
    }
}
