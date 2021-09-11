package us.ajg0702.leaderboards.boards;

import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.utils.spigot.Config;
import us.ajg0702.utils.spigot.Messages;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.UUID;

public class StatEntry {
	
	String player;
	String prefix;
	String suffix;

	UUID playerID;
	
	int position;
	String board;

	private Cache cache;
	private Messages msgs;

	String k = "k";
	String m = "m";
	String b = "b";
	String t = "t";
	String q = "q";
	
	double score;
	public StatEntry(int position, String board, String prefix, String player, UUID playerID, String suffix, double score) {
		this.player = player;
		this.score = score;
		this.prefix = prefix;
		this.suffix = suffix;

		this.playerID = playerID;

		try {
			this.cache = Cache.getInstance();
			msgs = cache.getPlugin().getMessages();
			k = msgs.get("formatted.k");
			m = msgs.get("formatted.m");
			b = msgs.get("formatted.b");
			t = msgs.get("formatted.t");
			q = msgs.get("formatted.q");
		} catch(NoClassDefFoundError ignored) {}
		
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

	public UUID getPlayerID() {
		return playerID;
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

	public String getScoreFormatted() {
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

		if (score < 1000L) {
			return formatNumber(score);
		}
		if (score < 1000000L) {
			return formatNumber(score/1000L)+k;
		}
		if (score < 1000000000L) {
			return formatNumber(score/1000000L)+m;
		}
		if (score < 1000000000000L) {
			return formatNumber(score/1000000000L)+b;
		}
		if (score < 1000000000000000L) {
			return formatNumber(score/1000000000000L)+t;
		}
		if (score < 1000000000000000000L) {
			return formatNumber(score/1000000000000000L)+q;
		}

		return getScorePretty();
	}

	private String formatNumber(double d) {
		NumberFormat format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(0);
		return format.format(d);
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
		return addCommas(score);
	}
	
	
	private String addCommas(double number) {
		String comma;
		if(cache != null) {
			comma = Cache.getInstance().getPlugin().getAConfig().getString("comma");
		} else { comma = ","; }
		DecimalFormat df = new DecimalFormat("#.##");
		String ns = df.format(number);
		int ic = 0;
		if(ns.indexOf(".") == ns.length()-2 && ns.charAt(ns.length()-1) == '0' && ns.length() >= 3) {
			ns = ns.substring(0, ns.length()-2);
		}
		String mn = ns.contains(".") ? ns.substring(0, ns.indexOf(".")) : ns;
		for(int i = mn.length()-1; i > 0; i--) {
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

		return ns;
	}
}
