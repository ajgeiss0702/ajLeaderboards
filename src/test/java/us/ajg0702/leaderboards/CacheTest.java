package us.ajg0702.leaderboards;

import org.junit.Test;

import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;

import java.util.UUID;

public class CacheTest {
    @Test
    public void testDecimal() throws Exception {
        StatEntry se = new StatEntry(1, "test_board", "testprefix", "testplayer", "testplayerdisplay", UUID.randomUUID(), "testsuffix", 1.5, TimedType.ALLTIME);
        //System.out.println("score pretty 1.5: "+se.getScorePretty());
        if(!se.getScorePretty().equals("1.5")) {
            throw new Exception("Score is "+se.getScorePretty()+" instead of 1.5");
        }
        System.out.println("Passed decimal test");
    }
    @Test
    public void testComma() throws Exception {
        StatEntry se = new StatEntry(1, "test_board", "testprefix", "testplayer", "testplayerdisplay", UUID.randomUUID(), "testsuffix", 1500, TimedType.ALLTIME);
        if(!se.getScorePretty().equals("1,500")) {
            throw new Exception("Score is "+se.getScorePretty()+" instead of 1,500");
        }
        System.out.println("Passed comma test");
    }
    @Test
    public void testCommaDecimal() throws Exception {
        StatEntry se = new StatEntry(1, "test_board", "testprefix", "testplayer", "testplayerdisplay", UUID.randomUUID(), "testsuffix", 1500.5, TimedType.ALLTIME);
        if(!se.getScorePretty().equals("1,500.5")) {
            throw new Exception("Score is "+se.getScorePretty()+" instead of 1,500.5");
        }
        System.out.println("Passed comma decimal test");
    }
}