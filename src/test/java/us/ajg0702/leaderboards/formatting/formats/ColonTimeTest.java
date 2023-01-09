package us.ajg0702.leaderboards.formatting.formats;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class ColonTimeTest extends TestCase {

    public void testMatches() throws Exception {
        List<String> shouldMatch = Arrays.asList(
                "00:00:00.00",
                "00:00:00:00",
                "1:2:3:4",
                "1:2:3.4",
                "11:22:33:44",
                "11:22:33.44",
                "12:3:15.735"
        );
        List<String> shouldNotMatch = Arrays.asList(
                "hello there",
                "15e",
                "12n 15b",
                "[60]",
                "123456",
                "13h 21m",
                ""
        );

        ColonTime colonTimeFormat = new ColonTime();

        for (String match : shouldMatch) {
            boolean res = colonTimeFormat.matches(match, "");
            if(!res) {
                throw new Exception(match + " did not match when it should have!");
            }
        }

        for (String notMatch : shouldNotMatch) {
            if(colonTimeFormat.matches(notMatch, "")) {
                throw new Exception(notMatch + " did match when it should not have!");
            }
        }
    }

}