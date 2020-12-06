package us.ajg0702.leaderboards.boards;

import us.ajg0702.leaderboards.Cache;
import us.ajg0702.utils.spigot.Config;

public class StatEntry {
	
	String player;
	String prefix;
	String suffix;
	
	
	double score;
	public StatEntry(String prefix, String player, String suffix, double score) {
		this.player = player;
		this.score = score;
		this.prefix = prefix;
		this.suffix = suffix;
	}
	
	public String getPrefix() {
		return prefix;
	}
	public String getSuffix() {
		return suffix;
	}
	
	public String getPlayer() {
		return player;
	}
	
	public double getScore() {
		return score;
	}
	
	public String getScorePretty() {
		Config config = Cache.getInstance().getPlugin().getAConfig();
		if(score == 0 && player.equals(config.getString("no-data-name"))) {
			return config.getString("no-data-score");
		}
		return addCommas(score);
	}
	
	
	private String addCommas(double number) {
		Config config = Cache.getInstance().getPlugin().getAConfig();
		String ns = number+"";
		int ic = 0;
		for(int i = ns.length(); i > 0; i--) {
			//System.out.println("i: "+i+" ic: "+ic + " c: " + ns.charAt(i-1));
			if(ns.contains(".") && i >= ns.indexOf(".")) continue;
			ic++;
			if(ic % 3 != 0) continue;
			ns = ns.substring(0, i)+config.getString("comma")+ns.substring(i, ns.length());
		}
		
		if(ns.indexOf(".") == ns.length()-2 && ns.charAt(ns.length()-1) == '0') {
			ns = ns.substring(0, ns.length()-2);
		}
		
		return ns;
		
		
		/*DecimalFormat df = new DecimalFormat("#.##");
		df.setGroupingUsed(true);
		df.setGroupingSize(3);
		return df.format(number);*/
	}
}
