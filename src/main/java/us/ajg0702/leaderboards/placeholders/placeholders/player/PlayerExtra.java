package us.ajg0702.leaderboards.placeholders.placeholders.player;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.regex.Matcher;

public class PlayerExtra extends Placeholder {
    public PlayerExtra(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "extra_(.*)";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String value = plugin.getTopManager().getExtra(p.getUniqueId(), matcher.group(1));
        if(value == null) {
            return plugin.getMessages().getRawString("no-data.extra");
        }
        return value;
    }
}
