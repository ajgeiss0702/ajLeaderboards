package us.ajg0702.leaderboards.boards;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum TimedType {
    ALLTIME, DAILY, WEEKLY, MONTHLY;

    public static List<String> lowerNames() {
        List<String> names = new ArrayList<>();
        for(TimedType type : values()) {
            names.add(type.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }
}
