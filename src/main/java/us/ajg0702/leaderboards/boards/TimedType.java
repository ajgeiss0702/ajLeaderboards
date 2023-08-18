package us.ajg0702.leaderboards.boards;


import java.time.DayOfWeek;
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

    private final String lowerName = name().toLowerCase(Locale.ROOT);

    public String lowerName() {
        return lowerName;
    }

    public static List<String> lowerNames() {
        List<String> names = new ArrayList<>();
        for(TimedType type : values()) {
            names.add(type.lowerName());
        }
        return names;
    }

    private static DayOfWeek weeklyResetDay = DayOfWeek.SUNDAY;
    public static void setWeeklyResetDay(DayOfWeek dayOfWeek) {
        weeklyResetDay = dayOfWeek;
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
                LocalDateTime weekly = now
                        .plusDays(weeklyResetDay.getValue() - now.getDayOfWeek().getValue())
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                if(weekly.isBefore(now)) {
                    weekly = weekly.plusWeeks(1);
                }
                return weekly;
            case MONTHLY:
                return now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEARLY:
                return now.plusYears(1).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                        .withNano(0);
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
                LocalDateTime weekly = now
                        .minusDays(now.getDayOfWeek().getValue() - weeklyResetDay.getValue())
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                if(weekly.isAfter(now)) {
                    weekly = weekly.minusWeeks(1);
                }
                return weekly;
            case MONTHLY:
                return now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEARLY:
                return now.withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        throw new IllegalStateException();
    }

    public static TimedType of(String string) {
        try {
            return TimedType.valueOf(string.toUpperCase(Locale.ROOT));
        } catch(IllegalArgumentException e) {
            return null;
        }
    }
}
