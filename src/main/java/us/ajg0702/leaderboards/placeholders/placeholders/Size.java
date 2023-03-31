package us.ajg0702.leaderboards.placeholders.placeholders;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.regex.Matcher;

import static us.ajg0702.leaderboards.boards.StatEntry.addCommas;

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
        int size = plugin.getTopManager().getBoardSize(board);
        if(size == -3) return "BDNE";
        if(size == -2) return plugin.getMessages().getString("loading.size");
        return addCommas(size);
    }
}
