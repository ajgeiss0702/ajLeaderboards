package us.ajg0702.leaderboards.placeholders.placeholders.lb;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.regex.Matcher;

public class Color extends Placeholder {
    public Color(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "lb_(.*)_([1-9][0-9]*)_(.*)_color";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        String typeRaw = matcher.group(3).toUpperCase();
        StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(matcher.group(2)), board, TimedType.valueOf(typeRaw));
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
