package us.ajg0702.leaderboards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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
	
	/*@EventHandler
	public void onSign(SignChangeEvent e) {
		Player player = e.getPlayer();
		
	}*/
}
