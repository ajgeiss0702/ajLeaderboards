package us.ajg0702.leaderboards.placeholders.placeholders;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.regex.Matcher;

public class Size extends Placeholder {
    public Size(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "size_(.*)";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        if(!plugin.getTopManager().boardExists(board)) {
            return "BDNE";
        }
        Integer count = plugin.getCache().getBoardSize(board);
        if(count == null) {
            return "...";
        }
        return count+"";
    }
}
