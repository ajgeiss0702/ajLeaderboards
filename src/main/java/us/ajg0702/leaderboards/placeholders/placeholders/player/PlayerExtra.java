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
        if(p == null) return "No player!";
        String value = plugin.getTopManager().getExtra(p.getUniqueId(), matcher.group(1));
        if(value == null) {
            // We only check if the extra is valid here because we want to support synced servers only gathering extras from a certain server
            if(!plugin.getExtraManager().isExtra(matcher.group(1))) {
                return "Extra does not exist";
            }
            return plugin.getMessages().getRawString("no-data.extra");
        }
        return value;
    }
}
