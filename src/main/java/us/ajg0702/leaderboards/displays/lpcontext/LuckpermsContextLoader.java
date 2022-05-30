package us.ajg0702.leaderboards.displays.lpcontext;

import us.ajg0702.leaderboards.LeaderboardPlugin;

public abstract class LuckpermsContextLoader {
    final LeaderboardPlugin plugin;
    boolean loaded = false;



    public LuckpermsContextLoader(LeaderboardPlugin leaderboardPlugin) {
        plugin = leaderboardPlugin;
    }

    public abstract void load();

    public abstract void checkReload();

    public boolean isLoaded() {
        return loaded;
    }
}
