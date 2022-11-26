package us.ajg0702.leaderboards.placeholders.placeholders.lb;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.Locale;
import java.util.regex.Matcher;

public class Extra extends Placeholder {
    public Extra(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "lb_(.*)_([1-9][0-9]*)_(.*)_extra_(.*)";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        String typeRaw = matcher.group(3).toUpperCase(Locale.ROOT);
        StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(matcher.group(2)), board, TimedType.valueOf(typeRaw));
        if(!r.hasPlayer()) {
            return plugin.getMessages().getRawString("no-data.extra");
        }
        String value = plugin.getTopManager().getExtra(r.getPlayerID(), matcher.group(4));
        if(value == null) {
            return plugin.getMessages().getRawString("no-data.extra");
        }
        return value;
    }
}
