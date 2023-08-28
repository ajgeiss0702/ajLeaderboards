package us.ajg0702.leaderboards;

import us.ajg0702.leaderboards.cache.Cache;

import java.util.logging.Logger;

public class Debug {

    private static boolean debug = false;
    private static Logger logger;

    public static void setLogger(Logger logger) {
        Debug.logger = logger;
    }

    public static void setDebug(boolean debug) {
        Debug.debug = debug;
        info("Debug mode "+(debug ? "enabled" : "disabled"));
    }

    public static void info(String message) {
        if(!debug) return;
        logger.info("[DEBUG] "+message);
    }

    public static boolean isDebug() {
        return debug;
    }

    private static boolean particles = false;

    public static void setParticles(boolean particles) {
        Debug.particles = particles;
    }
    public static boolean particles() {
        return particles;
    }
}
