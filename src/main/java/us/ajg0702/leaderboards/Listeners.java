package us.ajg0702.leaderboards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import us.ajg0702.leaderboards.signs.SignManager;

public class Listeners implements Listener {
	
	Main pl;
	public Listeners(Main pl) {
		this.pl = pl;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		Bukkit.getScheduler().runTaskAsynchronously(pl, new Runnable() {
			public void run() {
				Cache.getInstance().updatePlayerStats(p);
			}
		});
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSignBreak(BlockBreakEvent e) {
		if(e.isCancelled()) return;
		if(!e.getBlock().getType().toString().contains("SIGN")) return;
		Player player = e.getPlayer();
		if(SignManager.getInstance().removeSign(e.getBlock().getLocation())) {
			player.sendMessage(pl.msgs.color(
					"&aSuccessfully un-registered the ajLeaderboards sign you just broke"
			));
		}
	}
}
