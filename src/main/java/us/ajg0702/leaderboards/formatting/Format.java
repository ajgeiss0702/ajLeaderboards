package us.ajg0702.leaderboards.formatting;

public abstract class Format {
    public abstract boolean matches(String output, String placeholder);
    public abstract double toDouble(String input) throws NumberFormatException;
    public abstract String toFormat(double input);
}
