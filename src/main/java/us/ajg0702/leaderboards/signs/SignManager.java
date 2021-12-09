package us.ajg0702.leaderboards.signs;

import org.bukkit.Bukkit;
import us.ajg0702.leaderboards.LeaderboardPlugin;

public class SignManager {
    private final LeaderboardPlugin plugin;

    public SignManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTask(plugin, this::reload);
    }

    public void reload() {

    }
}
