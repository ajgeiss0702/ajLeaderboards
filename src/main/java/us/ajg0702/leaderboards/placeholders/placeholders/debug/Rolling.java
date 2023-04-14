package us.ajg0702.leaderboards.placeholders.placeholders.debug;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.util.Arrays;
import java.util.regex.Matcher;

public class Rolling extends Placeholder {
    public Rolling(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "rolling";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        StringBuilder builder = new StringBuilder();
        for (Integer i : plugin.getTopManager().getRolling()) {
            String is = i+"";
            if(is.length() == 1) {
                is = "__"+i;
            } else if(is.length() == 2) {
                is = "_"+i;
            }
            if(i == 0) {
                is = "___";
            }
            builder.append(is).append("-");
        }
        return builder.substring(0, builder.length()-1);
    }
}
