package us.ajg0702.leaderboards.displays.lpcontext;

import us.ajg0702.leaderboards.LeaderboardPlugin;

public class WithoutLPCtx extends LuckpermsContextLoader {
    public WithoutLPCtx(LeaderboardPlugin leaderboardPlugin) {
        super(leaderboardPlugin);
    }

    @Override
    public void load() {}

    @Override
    public void checkReload(boolean register) {}

    @Override
    public void calculatePotentialContexts() {}
}
