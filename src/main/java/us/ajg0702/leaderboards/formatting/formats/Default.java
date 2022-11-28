package us.ajg0702.leaderboards.formatting.formats;

import us.ajg0702.leaderboards.formatting.Format;

import static us.ajg0702.leaderboards.boards.StatEntry.addCommas;

public class Default extends Format {
    @Override
    public boolean matches(String output, String placeholder) {
        return true;
    }

    @Override
    public double toDouble(String input) {
        return Double.parseDouble(input);
    }

    @Override
    public String toFormat(double input) {
        return addCommas(input);
    }
}
