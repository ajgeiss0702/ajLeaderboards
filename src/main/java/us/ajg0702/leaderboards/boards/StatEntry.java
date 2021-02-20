package us.ajg0702.leaderboards.boards;

import java.text.DecimalFormat;

import us.ajg0702.leaderboards.Cache;
import us.ajg0702.utils.spigot.Config;

public class StatEntry {
	
	String player;
	String prefix;
	String suffix;
	
	int position;
	String board;
	
	
	double score;
	public StatEntry(int position, String board, String prefix, String player, String suffix, double score) {
		this.player = player;
		this.score = score;
		this.prefix = prefix;
		this.suffix = suffix;
		
		this.position = position;
		this.board = board;
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
	
	
	public int getPosition() {
		return position;
	}
	public String getBoard() {
		return board;
	}
	
	
	public double getScore() {
		return score;
	}
	
	public String getScorePretty() {
		Config config = Cache.getInstance().getPlugin().getAConfig();
		if(score == 0 && player.equals(config.getString("no-data-name"))) {
			return config.getString("no-data-score");
		}
		//Bukkit.getLogger().info("before: "+score);
		return addCommas(score);
	}
	
	
	private String addCommas(double number) {
		Config config = Cache.getInstance().getPlugin().getAConfig();
		DecimalFormat df = new DecimalFormat("#");
		df.setMaximumFractionDigits(0);
		String ns = df.format(number);
		int ic = 2;
		for(int i = ns.length(); i > 0; i--) {
			//System.out.println("i: "+i+" ic: "+ic + " c: " + ns.charAt(i-1));
			if(ns.contains(".") && i >= ns.indexOf(".")) continue;
			ic++;
			if(ic % 3 != 0) continue;
			ns = ns.substring(0, i)+config.getString("comma")+ns.substring(i, ns.length());
		}
		
		if(ns.indexOf(".") == ns.length()-2 && ns.charAt(ns.length()-1) == '0' && ns.length() >= 3) {
			ns = ns.substring(0, ns.length()-2);
		}
		
		if(ns.charAt(ns.length()-1) == ',') {
			ns = ns.substring(0, ns.length()-1);
		}
		
		//Bukkit.getLogger().info("after: "+ns);
		return ns;
		
		
		/*DecimalFormat df = new DecimalFormat("#.##");
		df.setGroupingUsed(true);
		df.setGroupingSize(3);
		return df.format(number);*/
	}
}
