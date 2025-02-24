package us.ajg0702.leaderboards.placeholders.placeholders.relative;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.Locale;
import java.util.regex.Matcher;

public class RelExtra extends Placeholder {
    public RelExtra(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "rel_(.*)_(.*)_([+-])([1-9][0-9]*)_extra_(.*)";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        String typeRaw = matcher.group(2).toUpperCase(Locale.ROOT);
        TimedType type = TimedType.of(typeRaw);
        if(type == null) {
            return "Invalid TimedType '" + typeRaw + "'";
        }
        String posneg = matcher.group(3);
        int position = Integer.parseInt(matcher.group(4));
        if(posneg.equals("-")) position *= -1;
        StatEntry r = plugin.getTopManager().getRelative(p, position, board, type);
        if(!r.hasPlayer()) {
            return plugin.getMessages().getRawString("no-data.rel.value");
        }
        String value = plugin.getTopManager().getExtra(r.getPlayerID(), matcher.group(4));
        if(value == null) {
            // We only check if the extra is valid here because we want to support synced servers only gathering extras from a certain server
            if(!plugin.getExtraManager().isExtra(matcher.group(4))) {
                return "Extra does not exist";
            }
            return plugin.getMessages().getRawString("no-data.rel.value");
        }
        return value;
    }
}
