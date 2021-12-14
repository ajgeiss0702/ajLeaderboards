package us.ajg0702.leaderboards.boards;

import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.UUID;

public class StatEntry {

	private final LeaderboardPlugin plugin;
	
	String player;
	String prefix;
	String suffix;

	UUID playerID;
	
	int position;
	String board;

	private Cache cache;

	private TimedType type;

	String k = "k";
	String m = "m";
	String b = "b";
	String t = "t";
	String q = "q";
	
	double score;
	public StatEntry(LeaderboardPlugin plugin, int position, String board, String prefix, String player, UUID playerID, String suffix, double score, TimedType type) {
		this.plugin = plugin;
		this.player = player;
		this.score = score;
		this.prefix = prefix;
		this.suffix = suffix;
		this.type = type;

		this.playerID = playerID;

		try {
			this.cache = plugin.getCache();
			Messages msgs = plugin.getMessages();
			k = msgs.getString("formatted.k");
			m = msgs.getString("formatted.m");
			b = msgs.getString("formatted.b");
			t = msgs.getString("formatted.t");
			q = msgs.getString("formatted.q");
		} catch(NoClassDefFoundError ignored) {}
		
		this.position = position;
		this.board = board;
	}

	public boolean hasPlayer() {
		return player.equals(plugin.getAConfig().getString("no-data-name")) && getPlayerID() != null;
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

	public TimedType getType() {
		return type;
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
			comma = plugin.getAConfig().getString("comma");
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
