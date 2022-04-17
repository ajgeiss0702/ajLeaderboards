package us.ajg0702.leaderboards.boards;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum TimedType {
    ALLTIME,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

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

    public LocalDateTime getNextReset() {
        LocalDateTime now = LocalDateTime.now();
        switch(this) {
            case ALLTIME:
                throw new IllegalStateException("ALLTIME doesnt have a reset date!");
            case HOURLY:
                return now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
            case DAILY:
                return now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case WEEKLY:
                return now.plusDays(7-now.getDayOfWeek().getValue()).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTHLY:
                return now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEARLY:
                return now.plusYears(1).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        throw new IllegalStateException();
    }
    public LocalDateTime getEstimatedLastReset() {
        LocalDateTime now = LocalDateTime.now();
        switch(this) {
            case ALLTIME:
                throw new IllegalStateException("ALLTIME doesnt have a reset date!");
            case HOURLY:
                return now.withMinute(0).withSecond(0).withNano(0);
            case DAILY:
                return now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            case WEEKLY:
                return now.minusDays(now.getDayOfWeek().getValue()).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTHLY:
                return now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEARLY:
                return now.withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        throw new IllegalStateException();
    }
}
