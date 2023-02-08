package us.ajg0702.leaderboards.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.*;

public class OfflineUpdater {
    private final Deque<OfflinePlayer> offlinePlayerQueue = new ArrayDeque<>();
    private final LeaderboardPlugin plugin;
    private final int started;
    private final String board;

    public OfflineUpdater(LeaderboardPlugin plugin, String board, OfflinePlayer[] players) {
        this.plugin = plugin;
        this.board = board;
        offlinePlayerQueue.addAll(Arrays.asList(players));
        started = offlinePlayerQueue.size();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            while(!offlinePlayerQueue.isEmpty() && !plugin.isShuttingDown()) {
                OfflinePlayer player = offlinePlayerQueue.pop();
                plugin.getCache().updateStat(board, player);
            }
            if(plugin.isShuttingDown()) {
                plugin.getLogger().info("[OfflineUpdater] " + board + ": Canceling due to plugin shutdown");
            } else {
                plugin.getLogger().info("[OfflineUpdater] " + board + ": Done!");
            }
            plugin.getOfflineUpdaters().remove(board, this);
        });
    }
    public double getProgressPercent() {
        return offlinePlayerQueue.size() / ((double) started);
    }
    public int getRemainingPlayers() {
        return offlinePlayerQueue.size();
    }
    public int getDonePlayers() {
        return started - offlinePlayerQueue.size();
    }

    public int getStarted() {
        return started;
    }

    public boolean isDone() {
        return getRemainingPlayers() == 0;
    }

    public void progressLog() {
        plugin.getLogger().info(
                "[OfflineUpdater] " + board + ": " +
                        Math.round(getProgressPercent() * 1000)/10 + "% done " +
                        "(" + getRemainingPlayers() + " / " + getStarted() + ")"
        );
    }
}
