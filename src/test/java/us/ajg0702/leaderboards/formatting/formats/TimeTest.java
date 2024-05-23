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
                "13 hours 21 minutes 15 seconds",
                "6d 15h 46m 8s",
                "9h 21m 44s",
                "2s",
                "1d 14h 40m",
                "0d 14h 40m",
                "9h 21m 44s",
                "2h 8m 37s"
        );
        List<String> shouldNotMatch = Arrays.asList(
                "hello there",
                "15e",
                "12n 15b",
                "[60]",
                "123456",
                ""
        );

        Time timeFormat = new Time(null);

        for (String match : shouldMatch) {
            boolean res = timeFormat.matches(match, "");
            if(!res) {
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