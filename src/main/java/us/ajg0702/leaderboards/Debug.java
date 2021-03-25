package us.ajg0702.leaderboards;

import us.ajg0702.leaderboards.Cache;
import us.ajg0702.leaderboards.Main;

public class Debug {
    public static void info(String message) {
        Main pl = Cache.getInstance().getPlugin();
        if(!pl.getAConfig().getBoolean("debug")) return;
        pl.getLogger().info("[DEBUG] "+message);
    }
}
