package us.ajg0702.leaderboards.placeholders.placeholders;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.regex.Matcher;

public class Fetching extends Placeholder {
    public Fetching(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "fetching";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        TopManager tm = plugin.getTopManager();
        return tm.getFetching()+" ("+tm.getActiveFetchers()+"+"+tm.getQueuedTasks()+"/"+tm.getMaxFetchers()+") "+tm.getFetchingAverage()+" "+tm.cacheTime();
    }
}
