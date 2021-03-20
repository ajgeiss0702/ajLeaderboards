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

	private Cache cache;
	
	
	double score;
	public StatEntry(int position, String board, String prefix, String player, String suffix, double score) {
		this.player = player;
		this.score = score;
		this.prefix = prefix;
		this.suffix = suffix;

		this.cache = Cache.getInstance();
		
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
		if(cache != null) {
			Config config = cache.getPlugin().getAConfig();
			if(score == 0 && player.equals(config.getString("no-data-name"))) {
				return config.getString("no-data-score");
			}
		} else {
			if(score == 0 && player.equals("---")) {
				return "---";
			}
		}
		//Bukkit.getLogger().info("before: "+score);
		return addCommas(score);
	}
	
	
	private String addCommas(double number) {
		String comma;
		if(cache != null) {
			comma = Cache.getInstance().getPlugin().getAConfig().getString("comma");
		} else { comma = ","; }
		DecimalFormat df = new DecimalFormat("#.##");
		//df.setMaximumFractionDigits(0);
		String ns = df.format(number);
		int ic = 0;
		if(ns.indexOf(".") == ns.length()-2 && ns.charAt(ns.length()-1) == '0' && ns.length() >= 3) {
			ns = ns.substring(0, ns.length()-2);
		}
		String mn = ns.contains(".") ? ns.substring(0, ns.indexOf(".")) : ns;
		for(int i = mn.length()-1; i > 0; i--) {
			//System.out.println("i: "+i+" ic: "+ic + " c: " + ns.charAt(i-1));
			ic++;
			if(ic % 3 != 0) continue;
			mn = mn.substring(0, i)+comma+mn.substring(i);
		}
		if(ns.contains(".")) {
			ns = mn+ns.substring(ns.indexOf("."));
		} else {
			ns = mn;
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
