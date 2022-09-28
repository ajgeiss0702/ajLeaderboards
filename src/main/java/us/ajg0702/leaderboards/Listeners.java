package us.ajg0702.leaderboards;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Listeners implements Listener {

    private final LeaderboardPlugin plugin;

    public Listeners(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(!plugin.getAConfig().getBoolean("update-stats")) return;
        if(!plugin.getAConfig().getBoolean("update-on-join")) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getCache().updatePlayerStats(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getCache().cleanPlayer(e.getPlayer());
    }
}
