package us.ajg0702.leaderboards;

import us.ajg0702.utils.common.Messages;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

@SuppressWarnings("unused")
public class TimeUtils {

    private static String w = "w ";
    private static String d = "d ";
    private static String h = "h ";
    private static String m = "m ";
    private static String s = "s";

    public static void setStrings(String w, String d, String h, String m, String s) {
        TimeUtils.w = w;
        TimeUtils.d = d;
        TimeUtils.h = h;
        TimeUtils.m = m;
        TimeUtils.s = s;
    }

    public static final long SECOND = 1000L;
    public static final long MINUTE = SECOND * 60L;
    public static final long HOUR = MINUTE * 60L;
    public static final long DAY = HOUR * 24L;
    public static final long WEEK = DAY * 7L;

    public static String formatTimeSeconds(long timeSeconds) {
        return formatTimeMs(timeSeconds*1000);
    }
    public static String formatTimeMs(long timeMs) {
        int weeks = (int) (timeMs / WEEK);
        int days = (int) ((timeMs % WEEK) / DAY);
        int hours = (int) ((timeMs % DAY) / HOUR);
        int minutes = (int) ((timeMs % HOUR) / MINUTE);
        int seconds = (int) ((timeMs % MINUTE) / SECOND);

        String weekss = weeks != 0 ? weeks+w : "";
        String dayss = days != 0 ? days+d : "";
        String hourss = hours != 0 ? hours+h : "";
        String minutess = minutes != 0 ? minutes+m : "";
        String secondss = seconds+s;

        return weekss+dayss+hourss+minutess+secondss;
    }

    public static ZoneOffset getDefaultZoneOffset() {
        return convertToZoneOffset(ZoneOffset.systemDefault());
    }
    public static ZoneOffset convertToZoneOffset(ZoneId zoneId) {
        return zoneId.getRules().getOffset(Instant.now());
    }

    public static void setStrings(Messages messages) {
        setStrings(
                messages.getString("time.w"),
                messages.getString("time.d"),
                messages.getString("time.h"),
                messages.getString("time.m"),
                messages.getString("time.s")
        );
    }
}
