package us.ajg0702.leaderboards.formatting;

import org.jetbrains.annotations.Nullable;

public abstract class Format {

    /**
     * Tests if the provided placeholder (and possibly output) is readable with this format
     * @param output The string output from the placeholder
     * @param placeholder the placeholder without %
     * @return True if this format supports parsing the output, false if it doesn't, or we're not sure
     */
    public abstract boolean matches(@Nullable String output, String placeholder);

    /**
     * Converts the provided input to a double using this format
     * @param input The string output from the placeholder
     * @return A double representing the string format
     * @throws NumberFormatException if the input does not match the format, or some other issue occurred with parsing
     */
    public abstract double toDouble(String input) throws NumberFormatException;

    /**
     * Converts a double into this format
     * @param input The provided double
     * @return A string that is the provided double in this format
     */
    public abstract String toFormat(double input);
}
