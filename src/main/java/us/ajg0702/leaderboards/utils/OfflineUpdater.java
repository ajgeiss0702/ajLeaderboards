package us.ajg0702.leaderboards.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import javax.annotation.Nullable;
import java.util.*;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class OfflineUpdater {
    private final Deque<OfflinePlayer> offlinePlayerQueue = new ArrayDeque<>();
    private final LeaderboardPlugin plugin;
    private final CommandSender reportTo;
    private final int started;
    private final String board;
    private long startedTime;

    public OfflineUpdater(LeaderboardPlugin plugin, String board, OfflinePlayer[] players, @Nullable CommandSender reportTo) {
        this.plugin = plugin;
        this.board = board;
        this.reportTo = reportTo;
        offlinePlayerQueue.addAll(Arrays.asList(players));
        started = offlinePlayerQueue.size();

        plugin.getScheduler().runTaskAsynchronously(() -> {
            startedTime = System.currentTimeMillis();
            while(!offlinePlayerQueue.isEmpty() && !plugin.isShuttingDown()) {
                OfflinePlayer player = offlinePlayerQueue.pop();
                plugin.getCache().updateStat(board, player);
            }
            if(plugin.isShuttingDown()) {
                plugin.getLogger().info("[OfflineUpdater] " + board + ": Canceling due to plugin shutdown");
            } else {
                long duration = System.currentTimeMillis() - startedTime;
                double durationSeconds = Math.round(duration / 10d) / 100d;
                plugin.getLogger().info("[OfflineUpdater] " + board + ": Finished in " + durationSeconds + "s " + duration);
                if(reportTo != null) {
                    reportTo.sendMessage(message(
                            "&aFinished updating all offline players for &f" + board + " &ain&f " + durationSeconds + "&as"
                    ));
                }
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
