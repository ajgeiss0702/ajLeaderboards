package us.ajg0702.leaderboards.formatting.formats;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class TimeTest extends TestCase {

    public void testMatches() throws Exception {
        List<String> shouldMatch = Arrays.asList(
                "12h 31m",
                "12w 2d 10h 51m 21s",
                "31m",
                "13 hours 21 minutes 15 seconds"
        );
        List<String> shouldNotMatch = Arrays.asList(
                "hello there",
                "15e",
                "12n 15b",
                "[60]"
        );

        Time timeFormat = new Time();

        for (String match : shouldMatch) {
            if(!timeFormat.matches(match, "")) {
                throw new Exception(match + " did not match when it should have!");
            }
        }

        for (String notMatch : shouldNotMatch) {
            if(timeFormat.matches(notMatch, "")) {
                throw new Exception(notMatch + " did match when it should not have!");
            }
        }
    }
}