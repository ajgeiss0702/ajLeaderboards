package us.ajg0702.leaderboards.boards;

import com.google.gson.JsonObject;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.TimeUtils;
import us.ajg0702.leaderboards.boards.keys.BoardType;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.utils.EasyJsonObject;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public class StatEntry {

	public static final String BOARD_DOES_NOT_EXIST = "Board does not exist";
	public static final String AN_ERROR_OCCURRED = "An error occurred";
	public static final String LOADING = "Loading";

	private static LeaderboardPlugin plugin;
	
	final String playerName;
	final String playerDisplayName;
	final String prefix;
	final String suffix;

	final UUID playerID;
	
	final int position;
	final String board;

	private Cache cache;

	private final TimedType type;

	String k = "k";
	String m = "m";
	String b = "b";
	String t = "t";
	String q = "q";
	
	final double score;
	final String scorePretty;
	public StatEntry(int position, String board, String prefix, String playerName, String playerDisplayName, UUID playerID, String suffix, double score, TimedType type) {
		this.playerName = playerName;
		this.playerDisplayName = playerDisplayName;
		this.score = score;
		this.prefix = prefix;
		this.suffix = suffix;
		this.type = type;

		this.playerID = playerID;

		if(plugin != null) {
			try {
				this.cache = plugin.getCache();
				Messages msgs = plugin.getMessages();
				k = msgs.getString("formatted.k");
				m = msgs.getString("formatted.m");
				b = msgs.getString("formatted.b");
				t = msgs.getString("formatted.t");
				q = msgs.getString("formatted.q");
			} catch(NoClassDefFoundError ignored) {}
		}
		
		this.position = position;
		this.board = board;

		scorePretty = calcPrettyScore();
	}
	private String calcPrettyScore() {
		if(cache != null) {
			Config config = cache.getPlugin().getAConfig();
			if(!hasPlayer()) {
				return config.getString("no-data-score");
			}
		} else {
			if(!hasPlayer()) {
				return "---";
			}
		}
		if(score == 0 && playerName.equals(BOARD_DOES_NOT_EXIST)) {
			return "BDNE";
		}
		return addCommas(score);
	}

	public boolean hasPlayer() {
		if(plugin == null) {
			return !playerName.equals("---") && getPlayerID() != null;
		}
		return !playerName.equals(plugin.getAConfig().getString("no-data-name")) && getPlayerID() != null;
	}
	
	public String getPrefix() {
		return prefix;
	}
	public String getSuffix() {
		return suffix;
	}
	
	public String getPlayerName() {
		return playerName;
	}

	public String getPlayerDisplayName() {
		return playerDisplayName;
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
		if(score == 0 && playerName.equals(BOARD_DOES_NOT_EXIST)) {
			return "BDNE";
		}
		if(!hasPlayer()) {
			if(cache != null) {
				return cache.getPlugin().getAConfig().getString("no-data-score");
			} else {
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
		return scorePretty;
	}

	public String getTime() {
		if(score == 0 && playerName.equals(BOARD_DOES_NOT_EXIST)) {
			return "BDNE";
		}
		if(!hasPlayer()) {
			return cache.getPlugin().getAConfig().getString("no-data-score");
		}
		return TimeUtils.formatTimeSeconds(Math.round(getScore()));
	}
	
	
	public static String addCommas(double number) {
		boolean useComma = true;
		char comma = 0;
		char decimal;
		if(plugin != null) {
			String commaString = plugin.getAConfig().getString("comma");
			useComma = !commaString.isEmpty();
			if(useComma) comma = commaString.charAt(0);
			decimal = plugin.getAConfig().getString("decimal").charAt(0);
		} else {
			comma = ',';
			decimal = '.';
		}
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.getDefault(Locale.Category.FORMAT));
		if(useComma) {
			symbols.setGroupingSeparator(comma);
		}
		symbols.setDecimalSeparator(decimal);
		DecimalFormat df = new DecimalFormat("#,###.##", symbols);
		df.setGroupingUsed(useComma);
		return df.format(number);
	}

	/**
	 * Deprecated. Use getPlayerName instead
	 */
	@Deprecated
	public String getPlayer() {
		return playerName;
	}


	public static StatEntry boardNotFound(LeaderboardPlugin plugin, int position, String board, TimedType type) {
		return new StatEntry(position, board, "", BOARD_DOES_NOT_EXIST, BOARD_DOES_NOT_EXIST, null, "", 0, type);
	}
	public static StatEntry error(LeaderboardPlugin plugin, int position, String board, TimedType type) {
		return new StatEntry(position, board, "", AN_ERROR_OCCURRED, AN_ERROR_OCCURRED, null, "", 0, type);
	}
	public static StatEntry noData(LeaderboardPlugin plugin, int position, String board, TimedType type) {
		return new StatEntry(position, board, "", plugin.getAConfig().getString("no-data-name"), plugin.getAConfig().getString("no-data-name"), null, "", -1, type);
	}
	public static StatEntry loading(LeaderboardPlugin plugin, String board, TimedType type) {
		return new StatEntry(-2, board, "", LOADING, LOADING, null, "", 0, type);
	}
	public static StatEntry loading(LeaderboardPlugin plugin, BoardType boardType) {
		return new StatEntry(-2, boardType.getBoard(), "", LOADING, LOADING, null, "", 0, boardType.getType());
	}
	public static StatEntry loading(LeaderboardPlugin plugin, OfflinePlayer player, BoardType boardType) {
		return new StatEntry(-2, boardType.getBoard(), "", player.getName(), player.getName(), player.getUniqueId(), "", 0, boardType.getType());
	}

	@SuppressWarnings("unused")
	public JsonObject toJsonObject() {
		return new EasyJsonObject()
				.add("playerName", playerName)
				.add("playerDisplayName", playerDisplayName)
				.add("prefix", prefix)
				.add("suffix", suffix)
				.add("playerID", playerID.toString())
				.add("position", position)
				.add("board", board)
				.add("type", type.toString())
				.add("score", score)
				.getHandle();
	}

	@SuppressWarnings("unused")
	public static StatEntry fromJsonObject(LeaderboardPlugin plugin, JsonObject object) {
		return new StatEntry(
				object.get("position").getAsInt(),
				object.get("board").getAsString(),
				object.get("prefix").getAsString(),
				object.get("playerName").getAsString(),
				object.get("playerDisplayName").getAsString(),
				UUID.fromString(object.get("playerID").getAsString()),
				object.get("suffix").getAsString(),
				object.get("score").getAsDouble(),
				TimedType.valueOf(object.get("type").getAsString().toUpperCase(Locale.ROOT))
		);
	}

	public static void setPlugin(LeaderboardPlugin leaderboardPlugin) {
		plugin = leaderboardPlugin;
	}
}
