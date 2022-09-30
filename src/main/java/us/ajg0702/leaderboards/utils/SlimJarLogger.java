package us.ajg0702.leaderboards.utils;

import io.github.slimjar.logging.ProcessLogger;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.text.MessageFormat;

public class SlimJarLogger implements ProcessLogger {
    private final LeaderboardPlugin plugin;

    public SlimJarLogger(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void log(String message, Object... args) {

        plugin.getLogger().info(MessageFormat.format(message, args));
    }

    @Override
    public void debug(String message, Object... args) {
        ProcessLogger.super.debug(message, args);
    }
}
