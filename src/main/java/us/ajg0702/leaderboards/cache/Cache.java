package us.ajg0702.leaderboards.cache;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.helpers.DbRow;
import us.ajg0702.leaderboards.cache.methods.H2Method;
import us.ajg0702.leaderboards.cache.methods.MongoDBMethod;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;
import us.ajg0702.leaderboards.cache.methods.SqliteMethod;
import us.ajg0702.leaderboards.utils.BoardPlayer;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@SuppressWarnings("FieldCanBeLocal")
public class Cache {
	public LeaderboardPlugin getPlugin() {
		return plugin;
	}

	ConfigFile storageConfig;
	final LeaderboardPlugin plugin;

	final CacheMethod method;

	List<String> nonExistantBoards = new ArrayList<>();

	public Cache(LeaderboardPlugin plugin) {
		this.plugin = plugin;

		if(plugin.getDataFolder().mkdirs()) {
			plugin.getLogger().info("Directory created");
		}

		try {
			storageConfig = new ConfigFile(plugin.getDataFolder(), plugin.getLogger(), "cache_storage.yml");
		} catch (ConfigurateException e) {
			plugin.getLogger().log(Level.SEVERE, "Error when loading cache storage config! The plugin may not work properly!", e);
		}

		String methodStr = storageConfig.getString("method");
		if (methodStr.equalsIgnoreCase("mysql")) {
			plugin.getLogger().info("Using MySQL for board cache. (" + methodStr + ")");
			method = new MysqlMethod(storageConfig);
		} else if (methodStr.equalsIgnoreCase("sqlite")) {
			plugin.getLogger().info("Using SQLite for board cache. (" + methodStr + ")");
			method = new SqliteMethod(storageConfig);
		} else if (methodStr.equalsIgnoreCase("mongodb")) {
			plugin.getLogger().info("Using MongoDB for board cache. (" + methodStr + ")");
			method = new MongoDBMethod(storageConfig);
		} else {
			plugin.getLogger().info("Using H2 flatfile for board cache. (" + methodStr + ")");
			method = new H2Method(storageConfig);
		}
		method.init(plugin, storageConfig, this);
	}


	public CacheMethod getMethod() {
		return method;
	}

	/**
	 * Get a stat. It is reccomended you use TopManager#getStat instead of this,
	 * unless it is of absolute importance that you have the most up-to-date information
	 *
	 * @param position The position to get
	 * @param board    The board
	 * @return The StatEntry representing the position of the board
	 */
	public StatEntry getStat(int position, String board, TimedType type) {
		if (!plugin.getTopManager().boardExists(board)) {
			if (!nonExistantBoards.contains(board)) {
				nonExistantBoards.add(board);
			}
			return StatEntry.boardNotFound(plugin, position, board, type);
		}
		try {
			return method.getStatEntry(position, board, type);
		} catch(SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to get stat of player:", e);
			return StatEntry.error(plugin, position, board, type);
		}
	}

	public List<Integer> rolling = new CopyOnWriteArrayList<>();

	public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
		if(!plugin.getTopManager().boardExists(board)) {
			if(!nonExistantBoards.contains(board)) {
				nonExistantBoards.add(board);
			}
			return StatEntry.boardNotFound(plugin, -3, board, type);
		}
		return method.getStatEntry(player, board, type);
	}

	public int getBoardSize(String board) {
		if(!plugin.getTopManager().boardExists(board)) {
			if(!nonExistantBoards.contains(board)) {
				nonExistantBoards.add(board);
			}
			return -3;
		}

		return method.getBoardSize(board);
	}

	public boolean createBoard(String name) {
		return method.createBoard(name);
	}

	public boolean removePlayer(String board, String playerName) {
		return method.removePlayer(board, playerName);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean boardExists(String board) {
		return getBoards().contains(board);
	}

	public List<String> getBoards() {
		return method.getBoards();
	}

	public List<String> getDbTableList() {
		return method.getDbTableList();
	}

	public boolean removeBoard(String board) {
		return method.removeBoard(board);
	}

	public void updatePlayerStats(OfflinePlayer player) {
		List<String> updatableBoards = plugin.getAConfig().getStringList("only-update");
		for(String b : plugin.getTopManager().getBoards()) {
			if(!updatableBoards.isEmpty() && !updatableBoards.contains(b)) continue;
			if(plugin.isShuttingDown()) return;
			if(player.isOnline() && player.getPlayer() != null) {
				if(plugin.getAConfig().getBoolean("enable-dontupdate-permission") && player.getPlayer().hasPermission("ajleaderboards.dontupdate."+b)) continue;
			}
			updateStat(b, player);
		}

		boolean updateDebug = plugin.getAConfig().getBoolean("update-de-bug");

		for (String extra : plugin.getExtraManager().getExtras()) {
			String value;
			try {
				value = PlaceholderAPI.setPlaceholders(player, "%"+extra+"%");
			} catch(Exception e) {
				plugin.getLogger().log(Level.WARNING, "Placeholder %"+extra+"% threw an error for "+player.getName()+":", e);
				continue;
			}
			if(updateDebug) Debug.info("Got '"+value+"' from extra "+extra+" for "+player.getName());

			if(value.equals("%" + extra + "%")) {
				plugin.getLogger().warning("Extra " + extra + " returned itself! (for " + player.getName() + ") Skipping.");
				continue;
			}

			String cached = plugin.getTopManager().getCachedExtra(player.getUniqueId(), extra);
			if(cached != null && cached.equals(value)) {
				if(updateDebug) Debug.info("Skipping updating extra of "+player.getName()+" for "+extra+" because their cached score is the same as their current score");
				continue;
			}

			plugin.getExtraManager().setExtra(player.getUniqueId(), extra, value);
		}
	}

	List<BoardPlayer> zeroPlayers = new CopyOnWriteArrayList<>();

	public void updateStat(String board, OfflinePlayer player) {
		if(!plugin.getTopManager().boardExists(board)) {
			return;
		}
		boolean debug = plugin.getAConfig().getBoolean("update-de-bug");
		String outputraw;
		double output;
		if(plugin.isShuttingDown()) return;
		try {
			outputraw = PlaceholderAPI.setPlaceholders(player, "%"+alternatePlaceholders(board)+"%")
					.replaceAll(",", "");
			output = plugin.getPlaceholderFormatter().toDouble(outputraw, board);
		} catch(NumberFormatException e) {
			if(debug) Debug.info("Placeholder %"+board+"% for "+player.getName()+" returned a non-number! Ignoring it. Message: " + e);
			return;
		} catch(Exception e) {
			plugin.getLogger().log(Level.WARNING, "Placeholder %"+board+"% for player "+player.getName()+" threw an error:", e);
			return;
		}
		if(debug) Debug.info("Placeholder "+board+" for "+player.getName()+" returned "+output);

		BoardPlayer boardPlayer = new BoardPlayer(board, player);

		String displayName = player.getName();
		if(player.isOnline() && player.getPlayer() != null) {
			displayName = player.getPlayer().getDisplayName();
		}

		String prefix = "";
		String suffix = "";
		if(plugin.hasVault() && player instanceof Player && plugin.getAConfig().getBoolean("fetch-prefix-suffix-from-vault")) {
			prefix = plugin.getVaultChat().getPlayerPrefix((Player)player);
			suffix = plugin.getVaultChat().getPlayerSuffix((Player)player);
		}

		StatEntry cached = plugin.getTopManager().getCachedStatEntry(player, board, TimedType.ALLTIME);
		if(cached != null && cached.hasPlayer() && cached.getScore() == output && cached.getPlayerDisplayName().equals(displayName) && cached.getPrefix().equals(prefix) && cached.getSuffix().equals(suffix)) {
			if(debug) Debug.info("Skipping updating of "+player.getName()+" for "+board+" because their cached score is the same as their current score");
			return;
		}

		if(plugin.getAConfig().getStringList("dont-add-zero").contains(board)) {
			if(output == 0) {
				Debug.info("Skipping " + player.getName() + " because they returned 0 for " + board + "(dont-add-zero)");
				return;
			}
		}

		if(plugin.getAConfig().getBoolean("require-zero-validation")) {
			if(output == 0 && !zeroPlayers.contains(boardPlayer)) {
				zeroPlayers.add(boardPlayer);
				Debug.info("Skipping "+player.getName()+" because they returned 0 for "+board);
				return;
			} else if(output == 0 && zeroPlayers.contains(boardPlayer)) {
				Debug.info("Not skipping "+player.getName()+" because they still returned 0 for "+board);
			} else if(output != 0) {
				zeroPlayers.remove(boardPlayer);
			}
		}

		method.upsertPlayer(board, player, output, displayName, prefix, suffix);
	}

	public long getLastReset(String board, TimedType type) {
		return method.getLastReset(board, type);
	}

	public void reset(String board, TimedType type) throws ExecutionException, InterruptedException {
		if(!plugin.getTopManager().boardExists(board)) return;

		if(!plugin.getAConfig().getBoolean("update-stats")) return;

		List<String> updatableBoards = plugin.getAConfig().getStringList("only-update");
		if(!updatableBoards.isEmpty() && !updatableBoards.contains(board)) return;

		long startTime = System.currentTimeMillis();
		LocalDateTime startDateTime = LocalDateTime.now();
		long newTime = startDateTime.atOffset(ZoneOffset.UTC).toEpochSecond() * 1000;
		Debug.info(board + " " + type + " " + startDateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME) + " " + newTime);
		if (type.equals(TimedType.ALLTIME)) {
			throw new IllegalArgumentException("Cannot reset ALLTIME!");
		}
		Debug.info("Resetting " + board + " " + type.lowerName() + " leaderboard");
		long lastReset = plugin.getTopManager().getLastReset(board, type) * 1000L;
		if (plugin.isShuttingDown()) {
			return;
		}
		Debug.info("last: " + lastReset + " gap: " + (startTime - lastReset));
		method.resetBoard(board, type, newTime);
		Debug.info("Reset of " + board + " " + type.lowerName() + " took " + (System.currentTimeMillis()-startTime)+"ms");
	}

	public void insertRows(String board, List<DbRow> rows) throws SQLException {
		method.insertRows(board, rows);
	}

	public List<DbRow> getRows(String board) throws SQLException {
		return method.getRows(board);
	}

	private static final HashMap<String, String> altPlaceholders = new HashMap<String, String>() {{
		put("ajpk_stats_highscore", "ajpk_stats_highscore_nocache");
		put("ajtr_stats_wins", "ajtr_stats_wins_nocache");
		put("ajtr_stats_losses", "ajtr_stats_losses_nocache");
		put("ajtr_stats_gamesplayed", "ajtr_stats_gamesplayer_nocache");
	}};
	public static String alternatePlaceholders(String board) {
		return altPlaceholders.getOrDefault(board, board);
	}

	/**
	 * Cleans a player from some variables to prevent memory leaks.
	 * Should only be called when the player logs out
	 * @param player the player to remove
	 */
	public void cleanPlayer(Player player) {
		zeroPlayers.removeIf(boardPlayer -> boardPlayer.getPlayer().equals(player));
	}

	public List<String> getNonExistantBoards() {
		return nonExistantBoards;
	}
}
