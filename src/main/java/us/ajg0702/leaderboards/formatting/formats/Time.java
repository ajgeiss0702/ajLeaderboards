package us.ajg0702.leaderboards.formatting.formats;

import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.TimeUtils;
import us.ajg0702.leaderboards.formatting.Format;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Time extends Format {
    private final static Pattern weekPattern = Pattern.compile("([1-9][0-9]*)w");
    private final static Pattern dayPattern = Pattern.compile("([1-9][0-9]*)d");
    private final static Pattern hourPattern = Pattern.compile("([1-9][0-9]*)h");
    private final static Pattern minutePattern = Pattern.compile("([1-9][0-9]*)m");
    private final static Pattern secondPattern = Pattern.compile("([1-9][0-9]*)s");

    private final static Pattern fullPattern = Pattern.compile("(([1-9][0-9]*)w)?(([1-9][0-9]*)d)?(([1-9][0-9]*)h)?(([1-9][0-9]*)m)?(([1-9][0-9]*)s)?");

    private final static Map<String, String> replaces = new HashMap<>();
    static {
        replaces.put("week", "w");
        replaces.put("weeks", "w");

        replaces.put("day", "d");
        replaces.put("days", "d");

        replaces.put("hour", "h");
        replaces.put("hours", "h");

        replaces.put("minute", "m");
        replaces.put("minutes", "m");

        replaces.put("second", "s");
        replaces.put("seconds", "s");
    }

    @Override
    public boolean matches(String output, String placeholder) {
        if(placeholder.equalsIgnoreCase("statistic_time_played")) {
            // don't bother with more expensive checks below if we know it's a time placeholder
            // let me know about any other placeholders that should be here!
            return true;
        }
        if(output == null) return false;
        if(output.isEmpty()) return false;
        String temp = output.replaceAll(",", "");

        for (Map.Entry<String, String> replacesEntry : replaces.entrySet()) {
            temp = temp.replace(replacesEntry.getKey(), replacesEntry.getValue());
        }

        boolean matches = fullPattern.matcher(temp.replaceAll(" ", "")).matches();
        Debug.info("[Format: Time] '" + output + "' matches: " + matches);
        return matches;
    }

    @Override
    public double toDouble(String input) {
        String temp = input.replaceAll(",", "");
        int seconds = -1;

        seconds = getSeconds(temp, 60*60*24*7, seconds, weekPattern);
        seconds = getSeconds(temp, 60*60*24, seconds, dayPattern);
        seconds = getSeconds(temp, 60*60, seconds, hourPattern);
        seconds = getSeconds(temp, 60, seconds, minutePattern);
        seconds = getSeconds(temp, 1, seconds, secondPattern);

        if(seconds == -1) throw new NumberFormatException("Unable to parse time from: '" + input + "'");
        return seconds;
    }

    private static int getSeconds(String output, int multiplier, int seconds, Pattern pattern) {
        Matcher matcher = pattern.matcher(output);
        if(matcher.find()) {
            if(seconds == -1) seconds = 0;
            seconds += Integer.parseInt(matcher.group(1))*multiplier;
        }
        return seconds;
    }

    @Override
    public String toFormat(double input) {
        return TimeUtils.formatTimeSeconds(Math.round(input));
    }
}
