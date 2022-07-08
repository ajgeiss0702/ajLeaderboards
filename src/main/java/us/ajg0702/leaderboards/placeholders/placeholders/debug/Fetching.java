package us.ajg0702.leaderboards.placeholders.placeholders.debug;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.logging.Level;
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
        try {
            TopManager tm = plugin.getTopManager();
            int add = 0;
            for (Integer i : plugin.getCache().rolling) {
                if(i == null) continue;
                add += i;
            }
            int rollingSize = plugin.getCache().rolling.size();
            if(rollingSize == 0) rollingSize = 1;
            return tm.getFetching()+" ("+tm.getActiveFetchers()+"+"+tm.getQueuedTasks()+" "+tm.getWorkers()+"/"+tm.getMaxFetchers()+") "+tm.getFetchingAverage()+" "+tm.cacheTime()+" "+(add/rollingSize);
        } catch(Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error while parsing fetching placeholder:", e);
            return null;
        }
    }
}
