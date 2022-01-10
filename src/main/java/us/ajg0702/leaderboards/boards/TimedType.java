package us.ajg0702.leaderboards.boards;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum TimedType {
    ALLTIME(-1), HOURLY(60 * 60 * 1000), DAILY(24 * 60 * 60 * 1000), WEEKLY(7 * 24 * 60 * 60 * 1000), MONTHLY(30L * 24 * 60 * 60 * 1000);

    private final long resetMs;

    TimedType(long resetMs) {
        this.resetMs = resetMs;
    }

    public long getResetMs() {
        return resetMs;
    }

    public String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static List<String> lowerNames() {
        List<String> names = new ArrayList<>();
        for(TimedType type : values()) {
            names.add(type.lowerName());
        }
        return names;
    }
}
