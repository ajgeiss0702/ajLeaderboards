package us.ajg0702.leaderboards.placeholders.placeholders.player;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.regex.Matcher;

public class PlayerValue extends Placeholder {
    public PlayerValue(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "value_(.*)_(.*)";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String board = matcher.group(1);
        String typeRaw = matcher.group(2).toUpperCase();
        return plugin.getTopManager().getStatEntry(p, board, TimedType.valueOf(typeRaw)).getScoreFormatted();
    }
}
