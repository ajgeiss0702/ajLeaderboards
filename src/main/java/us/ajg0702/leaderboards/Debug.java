package us.ajg0702.leaderboards;

import us.ajg0702.leaderboards.cache.Cache;

public class Debug {
    public static void info(String message) {
        Main pl = Cache.getInstance().getPlugin();
        if(!pl.getAConfig().getBoolean("debug")) return;
        pl.getLogger().info("[DEBUG] "+message);
    }
}
