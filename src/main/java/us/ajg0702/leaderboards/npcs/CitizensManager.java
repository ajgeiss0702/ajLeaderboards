package us.ajg0702.leaderboards.npcs;

import us.ajg0702.leaderboards.Main;

public class CitizensManager {
	static CitizensManager instance;
	public static CitizensManager getInstance() {
		return instance;
	}
	public static CitizensManager getInstance(Main pl) {
		if(instance == null) {
			instance = new CitizensManager(pl);
		}
		return instance;
	}
	
	Main pl;
	
	private CitizensManager(Main pl) {
		this.pl = pl;
	}
}
