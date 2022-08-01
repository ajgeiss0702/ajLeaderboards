package us.ajg0702.leaderboards.displays.lpcontext;

import us.ajg0702.leaderboards.LeaderboardPlugin;

public abstract class LuckpermsContextLoader {
    final LeaderboardPlugin plugin;
    boolean loaded = false;



    public LuckpermsContextLoader(LeaderboardPlugin leaderboardPlugin) {
        plugin = leaderboardPlugin;
    }

    public abstract void load();

    public void checkReload() {
        checkReload(plugin.getAConfig().getBoolean("register-lp-contexts"));
    }

    public abstract void checkReload(boolean register);

    public boolean isLoaded() {
        return loaded;
    }

    public abstract void calculatePotentialContexts();
}
