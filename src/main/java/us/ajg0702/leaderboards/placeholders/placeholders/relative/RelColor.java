package us.ajg0702.leaderboards.placeholders.placeholders.relative;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.Locale;
import java.util.regex.Matcher;

public class RelColor extends Placeholder {
    public RelColor(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "rel_(.*)_(.*)_([+-])([1-9][0-9]*)_color";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        String typeRaw = matcher.group(2).toUpperCase(Locale.ROOT);
        String posneg = matcher.group(3);
        int position = Integer.parseInt(matcher.group(4));
        if(posneg.equals("-")) position *= -1;
        StatEntry r = plugin.getTopManager().getRelative(p, position, board, TimedType.valueOf(typeRaw));
        if(r.getPrefix().isEmpty()) return "";
        String prefix = r.getPrefix();
        StringBuilder colors = new StringBuilder();
        int i = 0;
        for(char c : prefix.toCharArray()) {
            if(i == prefix.length()-1) break;
            if(c == '&' || c == '\u00A7') {
                colors.append(c);
                colors.append(prefix.charAt(i+1));
            }
            i++;
        }
        return colors.toString();
    }
}
