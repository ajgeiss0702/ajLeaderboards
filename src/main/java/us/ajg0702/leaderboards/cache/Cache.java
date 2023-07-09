package us.ajg0702.leaderboards.cache;

import com.google.common.collect.ImmutableMap;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.keys.BoardType;
import us.ajg0702.leaderboards.cache.helpers.DbRow;
import us.ajg0702.leaderboards.cache.methods.H2Method;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;
import us.ajg0702.leaderboards.cache.methods.SqliteMethod;
import us.ajg0702.leaderboards.utils.BoardPlayer;
import us.ajg0702.leaderboards.utils.Partition;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;

@SuppressWarnings("FieldCanBeLocal")
public class Cache {
	private String q = "'";

	private final String SELECT_POSITION = "select 'id','value','namecache','prefixcache','suffixcache','displaynamecache',"+deltaBuilder()+" from '%s' order by '%s' %s, namecache desc limit 1 offset %d";
	private final String SELECT_PLAYER = "select 'id','value','namecache','prefixcache','suffixcache','displaynamecache',"+deltaBuilder()+" from '%s' order by '%s' %s, namecache desc";
	private final String GET_POSITION = "/*%s*/with N as (select *,ROW_NUMBER() OVER (order by '%s' %s, namecache desc) as position from '%s') select 'id','value','namecache','prefixcache','suffixcache','displaynamecache',position,"+deltaBuilder()+" from N where 'id'=?";
	private final Map<String, String> CREATE_TABLE = ImmutableMap.of(
			"sqlite", "create table if not exists '%s' (id TEXT PRIMARY KEY, value DECIMAL(20, 2)"+columnBuilder("NUMERIC")+", namecache TEXT, prefixcache TEXT, suffixcache TEXT, displaynamecache TEXT)",
			"h2", "create table if not exists '%s' ('id' VARCHAR(36) PRIMARY KEY, 'value' DECIMAL(20, 2)"+columnBuilder("BIGINT")+", 'namecache' VARCHAR(16), 'prefixcache' VARCHAR(255), 'suffixcache' VARCHAR(255), 'displaynamecache' VARCHAR(512))",
			"mysql", "create table if not exists '%s' ('id' VARCHAR(36) PRIMARY KEY, 'value' DECIMAL(20, 2)"+columnBuilder("BIGINT")+", 'namecache' VARCHAR(16), 'prefixcache' TINYTEXT, 'suffixcache' TINYTEXT, 'displaynamecache' VARCHAR(512))"
	);
	private final String REMOVE_PLAYER = "delete from '%s' where 'namecache'=?";
	private final Map<String, String> LIST_TABLES = ImmutableMap.of(
			"sqlite", "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';"
	);
	private final String DROP_TABLE = "drop table '%s';";
	private final String INSERT_PLAYER = "insert into '%s' ('id', 'value', 'namecache', 'prefixcache', 'suffixcache', 'displaynamecache'"+tableBuilder()+") values (?, ?, ?, ?, ?, ?"+qBuilder()+")";
	private final String UPDATE_PLAYER = "update '%s' set 'value'=?, 'namecache'=?, 'prefixcache'=?, 'suffixcache'=?, 'displaynamecache'=?"+updateBuilder()+" where id=?";
	private final String QUERY_LASTTOTAL = "select '%s' from '%s' where id=?";
	private final String QUERY_LASTRESET = "select '%s' from '%s' limit 1";
	private final String QUERY_IDVALUE = "select id,'value' from '%s'";
	private final String UPDATE_RESET = "update '%s' set '%s'=?, '%s'=?, '%s'=? where id=?";
	private final String QUERY_ALL = "select * from '%s'";
	private final String CREATE_TIMESTAMP_INDEX = "create index %s_timestamp on '%s' (%s_timestamp)";



	public LeaderboardPlugin getPlugin() {
		return plugin;
	}

	ConfigFile storageConfig;
	final LeaderboardPlugin plugin;
	final CacheMethod method;

	final String tablePrefix;

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
		if(methodStr.equalsIgnoreCase("mysql")) {
			plugin.getLogger().info("Using MySQL for board cache. ("+methodStr+")");
			method = new MysqlMethod();
			tablePrefix = storageConfig.getString("table_prefix");
			q = "`";
		} else if(methodStr.equalsIgnoreCase("sqlite")) {
			plugin.getLogger().info("Using SQLite for board cache. ("+methodStr+")");
			method = new SqliteMethod();
			tablePrefix = "";
			q = "'";
		} else {
			plugin.getLogger().info("Using H2 flatfile for board cache. ("+methodStr+")");
			method = new H2Method();
			tablePrefix = "";
			q = "`";
		}
		method.init(plugin, storageConfig, this);
	}

	/**
	 * Get a stat. It is reccomended you use TopManager#getStat instead of this,
	 * unless it is of absolute importance that you have the most up-to-date information
	 * @param position The position to get
	 * @param board The board
	 * @return The StatEntry representing the position of the board
	 */
	public StatEntry getStat(int position, String board, TimedType type) {
		if(!plugin.getTopManager().boardExists(board)) {
			if(!nonExistantBoards.contains(board)) {
				nonExistantBoards.add(board);
			}
			return StatEntry.boardNotFound(plugin, position, board, type);
		}
		boolean reverse = plugin.getAConfig().getStringList("reverse-sort").contains(board);
		try {
			Connection conn = method.getConnection();
			String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
			PreparedStatement ps = conn.prepareStatement(String.format(
					method.formatStatement(SELECT_POSITION),
					tablePrefix+board,
					sortBy,
					reverse ? "asc" : "desc",
					position-1
			));

			ResultSet r = ps.executeQuery();

			StatEntry se = processData(r, sortBy, position, board, type);
			ps.close();
			method.close(conn);
			return se;
		} catch(SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to get stat of player:", e);
			return StatEntry.error(plugin, position, board, type);
		}
	}

	public List<Integer> rolling = new CopyOnWriteArrayList<>();

	private final Map<String, Integer> sortByIndexes = new ConcurrentHashMap<>();
	public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
		long start = System.currentTimeMillis();
		if(!plugin.getTopManager().boardExists(board)) {
			if(!nonExistantBoards.contains(board)) {
				nonExistantBoards.add(board);
			}
			return StatEntry.boardNotFound(plugin, -3, board, type);
		}
		boolean reverse = plugin.getAConfig().getStringList("reverse-sort").contains(board);
		StatEntry r = null;
		try {
			Connection conn = method.getConnection();
			String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
			PreparedStatement ps = conn.prepareStatement(String.format(
					method.formatStatement(GET_POSITION),
					board,
					sortBy,
					reverse ? "asc" : "desc",
					tablePrefix+board
			));

			ps.setString(1, player.getUniqueId().toString());

			ResultSet rs = ps.executeQuery();

			rs.next();

			String uuidraw = null;
			double value = -1;
			String name = "-Unknown-";
			String displayName = name;
			String prefix = "";
			String suffix = "";
			int position = -1;
			try {
				uuidraw = rs.getString(1);
				name = rs.getString(3);
				prefix = rs.getString(4);
				suffix = rs.getString(5);
				displayName = rs.getString(6);
				position = rs.getInt(7);
				value = rs.getDouble(sortByIndexes.computeIfAbsent(sortBy,
						k -> {
							try {
								Debug.info("Calculating (statentry) column for "+sortBy);
								return rs.findColumn(sortBy);
							} catch (SQLException e) {
								plugin.getLogger().log(Level.SEVERE, "Error while finding a column for "+sortBy, e);
								return -1;
							}
						}
				));
			} catch(SQLException e) {
				if(
						!e.getMessage().contains("ResultSet closed") &&
								!e.getMessage().contains("empty result set") &&
								!e.getMessage().contains("[2000-")
				) {
					throw e;
				}
			}
			if(prefix == null) prefix = "";
			if(suffix == null) suffix = "";
			if(displayName == null) displayName = name;
			if(uuidraw != null) {
				r = new StatEntry(position, board, prefix, name, displayName, UUID.fromString(uuidraw), suffix, value, type);
			}
			rs.close();
			ps.close();
			method.close(conn);
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to get position/value of player:", e);
			return StatEntry.error(plugin, -1, board, type);
		}
		rolling.add((int) (System.currentTimeMillis()-start));
		if(rolling.size() > 50) {
			rolling.remove(0);
		}
		if(r == null) {
			return StatEntry.noData(plugin, -1, board, type);
		}
		return r;
	}

	public int getBoardSize(String board) {
		if(!plugin.getTopManager().boardExists(board)) {
			if(!nonExistantBoards.contains(board)) {
				nonExistantBoards.add(board);
			}
			return -3;
		}

		Connection connection = null;
		ResultSet rs = null;

		int size;

		try {
			connection = method.getConnection();

			PreparedStatement ps = connection.prepareStatement(String.format(
					method.formatStatement("select COUNT(1) from '%s'"),
					tablePrefix+board
			));

			rs = ps.executeQuery();

			rs.next();

			size = rs.getInt(1);

		} catch (SQLException e) {
			if(
					!e.getMessage().contains("ResultSet closed") &&
							!e.getMessage().contains("empty result set") &&
							!e.getMessage().contains("[2000-")
			) {
				plugin.getLogger().log(Level.WARNING, "Unable to get size of board:", e);
				return -1;
			} else {
				return 0;
			}
		} finally {
			try {
				if(connection != null) method.close(connection);
				if(rs != null) rs.close();
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Error while closing resources from board size fetch:", e);
			}

		}

		return size;
	}

	public boolean createBoard(String name) {
		try {
			Connection conn = method.getConnection();
			PreparedStatement ps = conn.prepareStatement(method.formatStatement(String.format(
					CREATE_TABLE.get(method.getName()),
					tablePrefix+name
			)));

			ps.executeUpdate();

			ps.close();

			for (TimedType type : TimedType.values()) {
				if(type == TimedType.ALLTIME) continue;

				try {
					ps = conn.prepareStatement(method.formatStatement(String.format(
							CREATE_TIMESTAMP_INDEX,
							type.lowerName(),
							tablePrefix+name,
							type.lowerName()
					)));
					ps.executeUpdate();
				} catch(SQLException e) {
					if(!e.getMessage().contains("already exists") && !e.getMessage().contains("Duplicate key") ) throw e;
				}
				ps.close();
			}

			method.close(conn);
			plugin.getTopManager().fetchBoards();
			plugin.getContextLoader().calculatePotentialContexts();
			nonExistantBoards.remove(name);
			if(!plugin.getTopManager().boardExists(name)) {
				plugin.getLogger().warning("Failed to create board: It wasnt created, but there was no error!");
				return false;
			}
			return true;
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to create board:", e);
			if(e.getCause() != null) {
				plugin.getLogger().log(Level.WARNING, "Cause:", e);
			}
			return false;
		}
	}

	public boolean removePlayer(String board, String playerName) {

		try {
			Connection conn = method.getConnection();
			PreparedStatement ps = conn.prepareStatement(String.format(
					method.formatStatement(REMOVE_PLAYER),
					tablePrefix+board
			));

			ps.setString(1, playerName);

			ps.executeUpdate();

			ps.close();
			method.close(conn);
			return true;
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to remove player from board:", e);
			return false;
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean boardExists(String board) {
		return getBoards().contains(board);
	}

	public List<String> getBoards() {
		List<String> o = new ArrayList<>();

		for(String table : getDbTableList()) {
			if(table.indexOf(tablePrefix) != 0) continue;
			String name = table.substring(tablePrefix.length());
			if(name.equals("extras")) continue;
			o.add(name);
		}

		return o;
	}

	public List<String> getDbTableList() {
		List<String> b = new ArrayList<>();
		try {

			ResultSet r;
			Connection conn = method.getConnection();
			Statement statement = conn.createStatement();
			r = statement.executeQuery(
					method.formatStatement(
							LIST_TABLES.getOrDefault(method.getName(), "show tables;")
					)
			);
			while(r.next()) {
				String e = r.getString(1);
				if(e.indexOf(tablePrefix) != 0) continue;
				String name = e.substring(tablePrefix.length());
				if(name.equals("extras")) continue;
				b.add(e);
			}

			statement.close();
			r.close();
			method.close(conn);
		} catch(SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to get list of tables:", e);
		}
		return b;
	}

	public boolean removeBoard(String board) {
		if(!plugin.getTopManager().boardExists(board)) {
			plugin.getLogger().warning("Attempted to remove board that doesnt exist!");
			return false;
		}
		try {
			if(method instanceof SqliteMethod) {
				((SqliteMethod) method).newConnection();
			}
			Connection conn = method.getConnection();
			PreparedStatement ps = conn.prepareStatement(String.format(
					method.formatStatement(DROP_TABLE),
					tablePrefix+board
			));
			ps.executeUpdate();

			ps.close();
			method.close(conn);
			plugin.getTopManager().fetchBoards();
			plugin.getContextLoader().calculatePotentialContexts();
			if(plugin.getTopManager().boardExists(board)) {
				plugin.getLogger().warning("Attempted to remove a board, but it didnt get removed!");
				return false;
			}
			return true;
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "An error occurred while trying to remove a board:", e);
			return false;
		}
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
				plugin.getLogger().warning("Extra " + extra + " returned itself! (for " + player.getName() + ") Skipping. (This is an issue with the placeholder, not ajLeaderboards)");
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

		String displayName = player.getName();
		if(player.isOnline() && player.getPlayer() != null) {
			displayName = player.getPlayer().getDisplayName();
		}

		String prefix;
		String suffix;
		if(plugin.hasVault() && player instanceof Player && plugin.getAConfig().getBoolean("fetch-prefix-suffix-from-vault")) {
			prefix = plugin.getVaultChat().getPlayerPrefix((Player)player);
			suffix = plugin.getVaultChat().getPlayerSuffix((Player)player);
			if(prefix == null) {
				prefix = "";
				plugin.getLogger().warning("Got a null prefix for " + player.getName() + " from " + plugin.getVaultChat().getName());
			}
			if(suffix == null) {
				suffix = "";
				plugin.getLogger().warning("Got a null suffix for " + player.getName() + " from " + plugin.getVaultChat().getName());
			}
		} else {
			suffix = "";
			prefix = "";
		}

		String finalDisplayName = displayName;
		String finalSuffix = suffix;
		String finalPrefix = prefix;
		Runnable updateTask = () -> {

			BoardPlayer boardPlayer = new BoardPlayer(board, player);

			StatEntry cached = plugin.getTopManager().getCachedStatEntry(player, board, TimedType.ALLTIME);
			if(cached != null && cached.hasPlayer() &&
					cached.getScore() == output &&
					cached.getPlayerDisplayName().equals(finalDisplayName) &&
					cached.getPrefix().equals(finalPrefix) &&
					cached.getSuffix().equals(finalSuffix)
			) {
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

			Map<TimedType, Double> lastTotals = new HashMap<>();
			for(TimedType type : TimedType.values()) {
				if(type == TimedType.ALLTIME) continue;
				lastTotals.put(type, getLastTotal(board, player, type));
			}


			if(debug) Debug.info("Updating "+player.getName()+" on board "+board+" with values v: "+output+" suffix: "+ finalSuffix +" prefix: "+ finalPrefix);
			try(Connection conn = method.getConnection()) {
				try {
					PreparedStatement statement = conn.prepareStatement(String.format(
							method.formatStatement(INSERT_PLAYER),
							tablePrefix+board
					));
					if(debug) Debug.info("in try");
					statement.setString(1, player.getUniqueId().toString());
					statement.setDouble(2, output);
					statement.setString(3, player.getName());
					statement.setString(4, finalPrefix);
					statement.setString(5, finalSuffix);
					statement.setString(6, finalDisplayName);
					int i = 6;
					for(TimedType type : TimedType.values()) {
						if(type == TimedType.ALLTIME) continue;
						long lastReset = plugin.getTopManager().getLastReset(board, type)*1000;
						if(plugin.isShuttingDown()) {
							method.close(conn);
						}
						statement.setDouble(++i, 0);
						statement.setDouble(++i, output);
						statement.setLong(++i, lastReset == 0 ? System.currentTimeMillis() : lastReset);
					}

					statement.executeUpdate();
					statement.close();
					method.close(conn);
				} catch(SQLException e) {
					if(debug) Debug.info("in catch: " + e.getMessage());
					try(PreparedStatement statement = conn.prepareStatement(String.format(
							method.formatStatement(UPDATE_PLAYER),
							tablePrefix+board
					))) {
						statement.setDouble(1, output);
						statement.setString(2, player.getName());
						statement.setString(3, finalPrefix);
						statement.setString(4, finalSuffix);
						statement.setString(5, finalDisplayName);
						Map<TimedType, Double> timedTypeValues = new HashMap<>();
						timedTypeValues.put(TimedType.ALLTIME, output);
						int i = 6;
						for(TimedType type : TimedType.values()) {
							if(type == TimedType.ALLTIME) continue;
							double timedOut = output-lastTotals.get(type);
							timedTypeValues.put(type, timedOut);
							statement.setDouble(i++, timedOut);
						}
						for (Map.Entry<TimedType, Double> timedTypeDoubleEntry : timedTypeValues.entrySet()) {
							TimedType type = timedTypeDoubleEntry.getKey();
							double timedOut = timedTypeDoubleEntry.getValue();

							StatEntry statEntry = plugin.getTopManager().getCachedStatEntry(player, board, type, false);
							if(statEntry != null && player.getUniqueId().equals(statEntry.getPlayerID())) {
								statEntry.changeScore(timedOut, finalPrefix, finalSuffix);
							}

							Integer position = plugin.getTopManager()
									.positionPlayerCache.getOrDefault(player.getUniqueId(), new HashMap<>())
									.get(new BoardType(board, type));
							if(position != null) {
								StatEntry stat = plugin.getTopManager().getCachedStat(position, board, type);
								if(stat != null && player.getUniqueId().equals(stat.getPlayerID())) {
									stat.changeScore(timedOut, finalPrefix, finalSuffix);
								}
							}
						}
						statement.setString(i, player.getUniqueId().toString());
						statement.executeUpdate();
					}
				}
			} catch(SQLException e) {
				if(plugin.isShuttingDown()) return;
				plugin.getLogger().log(Level.WARNING, "Unable to update stat for player:", e);
			}
		};

		if(Bukkit.isPrimaryThread()) {
			plugin.getTopManager().submit(updateTask);
		} else {
			updateTask.run();
		}
	}

	public double getLastTotal(String board, OfflinePlayer player, TimedType type) {
		double last = 0;
		try {
			Connection conn = method.getConnection();
			try {
				PreparedStatement ps = conn.prepareStatement(String.format(
						method.formatStatement(QUERY_LASTTOTAL),
						type.lowerName()+"_lasttotal",
						tablePrefix+board
				));

				ps.setString(1, player.getUniqueId().toString());

				ResultSet rs = ps.executeQuery();

				if(method instanceof MysqlMethod || method instanceof H2Method) {
					rs.next();
				}
				last = rs.getDouble(1);
				rs.close();
				ps.close();
				method.close(conn);
			} catch(SQLException e) {
				method.close(conn);
				String m = e.getMessage();
				if(m.contains("empty result set") || m.contains("ResultSet closed") || m.contains("[2000-")) return last;
				plugin.getLogger().log(Level.WARNING, "Unable to get last total for "+player.getName()+" on "+type+" of "+board, e);
			}
		} catch(SQLException ignored) {}

		return last;
	}

	public long getLastReset(String board, TimedType type) {
		long last = 0;
		try {
			Connection conn = method.getConnection();
			try {
				PreparedStatement ps = conn.prepareStatement(String.format(
						method.formatStatement(QUERY_LASTRESET),
						type.lowerName()+"_timestamp",
						tablePrefix+board
				));

				ResultSet rs = ps.executeQuery();
				if(method instanceof MysqlMethod || method instanceof H2Method) {
					rs.next();
				}
				last = rs.getLong(1);
				ps.close();
				method.close(conn);
			} catch(SQLException e) {
				method.close(conn);
				String m = e.getMessage();
				if(m.contains("empty result set") || m.contains("ResultSet closed") || m.contains("[2000-")) return last;
				plugin.getLogger().log(Level.WARNING, "Unable to get last reset for "+type+" of "+board, e);
			}
		} catch(SQLException ignored) {}

		return last;
	}

	public void reset(String board, TimedType type) throws ExecutionException, InterruptedException {
		if(!plugin.getTopManager().boardExists(board)) return;

		if(!plugin.getAConfig().getBoolean("update-stats")) return;

		List<String> updatableBoards = plugin.getAConfig().getStringList("only-update");
		if(!updatableBoards.isEmpty() && !updatableBoards.contains(board)) return;

		long startTime = System.currentTimeMillis();
		LocalDateTime startDateTime = LocalDateTime.now();
		long newTime = startDateTime.atOffset(ZoneOffset.UTC).toEpochSecond()*1000;
		Debug.info(board+" "+type+" "+startDateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)+" "+newTime);
		if(type.equals(TimedType.ALLTIME)) {
			throw new IllegalArgumentException("Cannot reset ALLTIME!");
		}


		List<String> saveableTypes = plugin.getAConfig().getStringList("reset-save-types");
		if(saveableTypes.contains(type.toString()) || saveableTypes.contains("*")) {
			plugin.getResetSaver().save(board, type);
		}


		Debug.info("Resetting "+board+" "+type.lowerName()+" leaderboard");
		long lastReset = plugin.getTopManager().getLastReset(board, type)*1000L;
		if(plugin.isShuttingDown()) {
			return;
		}
		Debug.info("last: "+lastReset+" gap: "+(startTime - lastReset));
		String t = type.lowerName();
		try {
			Connection conn = method.getConnection();
			PreparedStatement ps = conn.prepareStatement(String.format(
					method.formatStatement(QUERY_IDVALUE),
					tablePrefix+board
			));

			ResultSet rs = ps.executeQuery();
			Map<String, Double> uuids = new HashMap<>();
			while(rs.next()) {
				uuids.put(rs.getString(1), rs.getDouble(2));
			}
			rs.close();
			ps.close();
			method.close(conn);
			Partition<String> partition = Partition.ofSize(new ArrayList<>(uuids.keySet()), Math.max(uuids.size()/(int) Math.ceil(method.getMaxConnections()/2D), 1));
			Debug.info("Partition length: "+partition.size()+" uuids size: "+ uuids.size()+" partition chunk size: "+partition.getChunkSize());
			for(List<String> uuidPartition : partition) {
				if(plugin.isShuttingDown()) {
					method.close(conn);
					return;
				}
				try {
					Connection con = method.getConnection();
					for(String idRaw : uuidPartition) {
						if(plugin.isShuttingDown()) {
							method.close(con);
							return;
						}
						PreparedStatement p = con.prepareStatement(String.format(
								method.formatStatement(UPDATE_RESET),
								tablePrefix+board,
								t+"_lasttotal",
								t+"_delta",
								t+"_timestamp"
						));
						p.setDouble(1, uuids.get(idRaw));
						p.setDouble(2, 0);
						p.setLong(3, newTime);
						p.setString(4, idRaw);
						p.executeUpdate();
						p.close();
					}
					method.close(con);
				} catch (SQLException e) {
					plugin.getLogger().log(Level.WARNING, "An error occurred while resetting "+type+" of "+board+":", e);
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "An error occurred while resetting "+type+" of "+board+":", e);
		}
		Debug.info("Reset of "+board+" "+type.lowerName()+" took "+(System.currentTimeMillis()-startTime)+"ms");
	}

	public void insertRows(String board, List<DbRow> rows) throws SQLException {
		Connection conn = method.getConnection();
		for(DbRow row : rows) {
			PreparedStatement statement = conn.prepareStatement(String.format(
					method.formatStatement(INSERT_PLAYER),
					tablePrefix+board
			));
			statement.setString(1, row.getId().toString());
			statement.setDouble(2, row.getValue());
			statement.setString(3, row.getNamecache());
			statement.setString(4, row.getPrefixcache());
			statement.setString(5, row.getSuffixcache());
			statement.setString(6, row.getDisplaynamecache());
			int i = 6;
			for(TimedType type : TimedType.values()) {
				if(type == TimedType.ALLTIME) continue;
				if(plugin.isShuttingDown()) {
					method.close(conn);
				}
				statement.setDouble(++i, row.getDeltas().get(type));
				statement.setDouble(++i, row.getLastTotals().get(type));
				statement.setLong(++i, row.getTimestamps().get(type));
			}

			try {
				statement.executeUpdate();
			} catch(SQLException e) {
				if(e.getMessage().contains("23505") || e.getMessage().contains("Duplicate entry") || e.getMessage().contains("PRIMARY KEY constraint failed")) {
					statement.close();
					continue;
				}
				throw e;
			}
			statement.close();
		}
		method.close(conn);
	}

	public List<DbRow> getRows(String board) throws SQLException {
		Connection conn = method.getConnection();
		PreparedStatement ps = conn.prepareStatement(String.format(
				method.formatStatement(QUERY_ALL),
				tablePrefix+board
		));
		ResultSet resultSet = ps.executeQuery();

		List<DbRow> out = new ArrayList<>();

		while(resultSet.next()) {
			out.add(new DbRow(resultSet));
		}

		ps.close();
		resultSet.close();
		method.close(conn);
		return out;
	}

	public CacheMethod getMethod() {
		return method;
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

	public String getTablePrefix() {
		return tablePrefix;
	}

	private String deltaBuilder() {
		StringBuilder deltaBuilder = new StringBuilder();
		for(TimedType t : TimedType.values()) {
			if(t == TimedType.ALLTIME) continue;
			deltaBuilder.append(q).append(t.lowerName()).append("_delta").append(q).append(",");
		}
		return deltaBuilder.deleteCharAt(deltaBuilder.length()-1).toString();
	}
	private String columnBuilder(String t) {
		String q = "'";
		StringBuilder columns = new StringBuilder();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			columns
					.append(",\n").append(q).append(type.lowerName()).append("_delta").append(q).append(" ").append(t)
					.append(",\n").append(q).append(type.lowerName()).append("_lasttotal").append(q).append(" ").append(t)
					.append(",\n").append(q).append(type.lowerName()).append("_timestamp").append(q).append(" ").append(t);
		}
		return columns.toString();
	}
	private String qBuilder() {
		StringBuilder addQs = new StringBuilder();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			addQs.append(", ?").append(", ?").append(", ?");
		}
		return addQs.toString();
	}

	private String tableBuilder() {
		StringBuilder addTables = new StringBuilder();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			String name = type.lowerName();
			addTables
					.append(", ").append(name).append("_delta")
					.append(", ").append(name).append("_lasttotal")
					.append(", ").append(name).append("_timestamp");
		}
		return addTables.toString();
	}

	private String updateBuilder() {
		StringBuilder addUpdates = new StringBuilder();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			String name = type.lowerName();
			addUpdates
					.append(", ").append(name).append("_delta").append("=?");
		}
		return addUpdates.toString();
	}

	Map<String, Integer> dataSortByIndexes = new ConcurrentHashMap<>();
	private StatEntry processData(ResultSet r, String sortBy, int position, String board, TimedType type) throws SQLException {
		String uuidRaw = null;
		double value = -1;
		String name = "-Unknown-";
		String displayName = name;
		String prefix = "";
		String suffix = "";
		if(method instanceof MysqlMethod || method instanceof H2Method) {
			r.next();
		}
		try {
			uuidRaw = r.getString(1);
			name = r.getString(3);
			prefix = r.getString(4);
			suffix = r.getString(5);
			displayName = r.getString(6);
			value = r.getDouble(dataSortByIndexes.computeIfAbsent(sortBy,
					k -> {
						try {
							Debug.info("Calculating (process) column for "+sortBy);
							return r.findColumn(sortBy);
						} catch (SQLException e) {
							plugin.getLogger().log(Level.SEVERE, "Error while finding a column for "+sortBy, e);
							return -1;
						}
					}
			));
		} catch(SQLException e) {
			if(
					!e.getMessage().contains("ResultSet closed") &&
							!e.getMessage().contains("empty result set") &&
							!e.getMessage().contains("[2000-")
			) {
				throw e;
			}
		}
		if(name == null) name = "-Unknown";
		r.close();

		if(prefix == null) prefix = "";
		if(suffix == null) prefix = "";
		if(displayName == null) displayName = name;

		if(uuidRaw == null) {
			return StatEntry.noData(plugin, position, board, type);
		} else {
			return new StatEntry(position, board, prefix, name, displayName, UUID.fromString(uuidRaw), suffix, value, type);
		}
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
