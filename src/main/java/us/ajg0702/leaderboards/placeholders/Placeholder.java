package us.ajg0702.leaderboards.placeholders;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Placeholder {

    protected final LeaderboardPlugin plugin;

    private Pattern pattern;

    public Placeholder(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract String getRegex();

    public Pattern getPattern() {
        if(pattern == null) {
            pattern = Pattern.compile(getRegex());
        }
        return pattern;
    }

    public abstract String parse(Matcher matcher, OfflinePlayer p);
}
