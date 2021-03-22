package us.ajg0702.leaderboards;

import org.junit.Test;

import us.ajg0702.leaderboards.boards.StatEntry;

public class CacheTest {
    @Test
    public void testDecimal() throws Exception {
        StatEntry se = new StatEntry(1, "test_board", "testplayer", "testprefix", "testsuffix", 1.5);
        //System.out.println("score pretty 1.5: "+se.getScorePretty());
        if(!se.getScorePretty().equals("1.5")) {
            throw new Exception("Score is "+se.getScorePretty()+" instead of 1.5");
        }
        System.out.println("Passed decimal test");
    }
    @Test
    public void testComma() throws Exception {
        StatEntry se = new StatEntry(1, "test_board", "testplayer", "testprefix", "testsuffix", 1500);
        if(!se.getScorePretty().equals("1,500")) {
            throw new Exception("Score is "+se.getScorePretty()+" instead of 1,500");
        }
        System.out.println("Passed comma test");
    }
    @Test
    public void testCommaDecimal() throws Exception {
        StatEntry se = new StatEntry(1, "test_board", "testplayer", "testprefix", "testsuffix", 1500.5);
        if(!se.getScorePretty().equals("1,500.5")) {
            throw new Exception("Score is "+se.getScorePretty()+" instead of 1,500.5");
        }
        System.out.println("Passed comma decimal test");
    }
}