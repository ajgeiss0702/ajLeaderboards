package us.ajg0702.leaderboards.formatting.formats;

import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.formatting.Format;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColonTime extends Format {

    private final Pattern pattern = Pattern.compile("(([0-9]*):)?([0-9]*):([0-9]*)(:|.)?([0-9]*)?");
    @Override
    public boolean matches(String output, String placeholder) {
        if(output == null) return false;
        boolean matches = pattern.matcher(output).matches();
        Debug.info("[Format: ColonTime] '" + output + "' matches: " + matches);
        return matches;
    }

    @Override
    public double toDouble(String input) throws NumberFormatException {
        Matcher matcher = pattern.matcher(input);
        if(!matcher.matches()) {
            Debug.info("[Format: ColonTime] Matcher in toDouble does not match!");
            throw new NumberFormatException("For input: " + input);
        }

        int hours = Integer.parseInt(matcher.group(2) == null ? "0" : matcher.group(2));
        int minutes = Integer.parseInt(matcher.group(3));
        int seconds = Integer.parseInt(matcher.group(4));

        String secondSeparator = matcher.group(5);
        String milliseconds = matcher.group(6);

        double result = 0;

        result += seconds;
        result += minutes * 60;
        result += hours * 60 * 60;

        if(secondSeparator != null && milliseconds != null) {
            if(secondSeparator.equals(":")) {
                result += Integer.parseInt(milliseconds) / 1000d;
            } else if(secondSeparator.equals(".")) {
                result += Double.parseDouble("0." + milliseconds);
            }
        }

        return result;

    }

    @Override
    public String toFormat(double input) {
        int hours = (int) (input / (60 * 60));
        int minutes = (int) ((input % (60 * 60)) / 60);
        double seconds = input % 60d;

        return
                (hours == 0 ? "00" : addZero(hours)) + ":" +
                (minutes == 0 ? "00" : addZero(minutes)) + ":" +
                (seconds == 0 ? "00" : addZero(seconds));
    }

    @Override
    public String getName() {
        return "ColonTime";
    }

    private String addZero(int i) {
        if(i <= 9 && i >= 0) {
            return "0" + i;
        }
        return i + "";
    }

    DecimalFormat decimalFormat = new DecimalFormat("#,#00.###");

    private String addZero(double d) {
        return decimalFormat.format(d);
    }
}
