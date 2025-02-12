package us.ajg0702.leaderboards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Listeners implements Listener {

    private final LeaderboardPlugin plugin;

    public Listeners(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(plugin.getCache().getMethod().getName().equals("sqlite") && e.getPlayer().hasPermission("ajleaderboards.use")) {
            plugin.getScheduler().runTaskLaterAsynchronously(() -> {
                plugin.getAdventure().player(e.getPlayer())
                        .sendMessage(message(
                                "\n&6[ajLeaderboards] &cSQLite is not recommended and will be removed! &7Please switch to h2 for a faster (and more stable) cache storage.\n" +
                                        "&cSQLite support will be removed in the future!\n" +
                                        "&7See how to switch without losing data " +
                                        "<hover:show_text:'<yellow>Click to go to https://wiki.ajg0702.us/ajleaderboards/moving-storage-methods'>" +
                                        "<click:open_url:'https://wiki.ajg0702.us/ajleaderboards/moving-storage-methods'>" +
                                        "<white><underlined>here (click me)" +
                                        "</click>" +
                                        "</hover>\n"
                        ));
            }, 40);
        }
        if(!plugin.getAConfig().getBoolean("update-stats")) return;
        if(!plugin.getAConfig().getBoolean("update-on-join")) return;
        plugin.getScheduler().runTaskAsynchronously(() -> plugin.getCache().updatePlayerStats(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getCache().cleanPlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST) // Lowest is run first
    public void onQuitFirst(PlayerQuitEvent e) {
        if(!plugin.getAConfig().getBoolean("update-stats")) return;
        if(!plugin.getAConfig().getBoolean("update-on-leave")) return;
        plugin.getCache().updatePlayerStats(e.getPlayer());
    }
}
