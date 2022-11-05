package us.ajg0702.leaderboards.placeholders.placeholders;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.TimeUtils;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.placeholders.Placeholder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;

public class Reset extends Placeholder {
    public Reset(LeaderboardPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getRegex() {
        return "reset_(.*)";
    }

    @Override
    public String parse(Matcher matcher, OfflinePlayer p) {
        String rawType = matcher.group(1);
        TimedType type;
        try {
            type = TimedType.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch(IllegalArgumentException ignored) {
            return "Invalid type";
        }
        long timeTilReset = LocalDateTime.now().until(type.getNextReset(), ChronoUnit.SECONDS);
        return TimeUtils.formatTimeSeconds(timeTilReset);
    }
}
