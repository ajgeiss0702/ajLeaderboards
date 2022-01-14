package us.ajg0702.leaderboards;

@SuppressWarnings("unused")
public class TimeUtils {

    private static final long SECOND = (1000L * 60);
    private static final long MINUTE = SECOND * 60L;
    private static final long HOUR = MINUTE * 60L;
    private static final long DAY = HOUR * 24L;
    private static final long WEEK = DAY * 7L;

    public static String formatTimeSeconds(long timeSeconds) {
        return formatTimeMs(timeSeconds*1000);
    }
    public static String formatTimeMs(long timeMs) {
        int weeks = (int) (timeMs / WEEK);
        int days = (int) ((timeMs % WEEK) / DAY);
        int hours = (int) ((timeMs % DAY) / HOUR);
        int minutes = (int) ((timeMs % HOUR) / MINUTE);
        int seconds = (int) ((timeMs % MINUTE) / SECOND);

        String weekss = weeks != 0 ? weeks+"w" : "";
        String dayss = days != 0 ? days+"d " : "";
        String hourss = hours != 0 ? hours+"h " : "";
        String minutess = minutes != 0 ? minutes+"m " : "";
        String secondss = seconds+"s";

        return weekss+dayss+hourss+minutess+secondss;
    }
}
