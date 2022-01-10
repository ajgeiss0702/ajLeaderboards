package us.ajg0702.leaderboards;

@SuppressWarnings("unused")
public class TimeUtils {
    public static String formatTimeSeconds(long timeSeconds) {
        return formatTimeMs(timeSeconds*1000);
    }
    public static String formatTimeMs(long timeMs) {
        int days = (int) (timeMs / (1000L * 60L * 60L * 24L));
        int hours = (int) (timeMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        int minutes = (int) (timeMs % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) (timeMs % (1000 * 60)) / 1000;

        String dayss = days != 0 ? days+"d " : "";
        String hourss = hours != 0 ? hours+"h " : "";
        String minutess = minutes != 0 ? minutes+"m " : "";
        String secondss = seconds+"s";

        return dayss+hourss+minutess+secondss;
    }
}
