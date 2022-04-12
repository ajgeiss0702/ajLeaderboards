package us.ajg0702.leaderboards.cache;

import com.google.common.collect.ImmutableMap;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.TimeUtils;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.methods.H2Method;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;
import us.ajg0702.leaderboards.cache.methods.SqliteMethod;
import us.ajg0702.leaderboards.utils.Partition;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static us.ajg0702.leaderboards.LeaderboardPlugin.convertPlaceholderOutput;

public class Cache {
	private String q = "'";

	private final String SELECT_POSITION = "select 'id','value','namecache','prefixcache','suffixcache','displaynamecache',"+deltaBuilder()+" from '%s' order by '%s' desc, namecache desc limit 1 offset %d";
	private final String SELECT_PLAYER = "select 'id','value','namecache','prefixcache','suffixcache','displaynamecache',"+deltaBuilder()+" from '%s' order by '%s' desc, namecache desc";
	private final Map<String, String> CREATE_TABLE = ImmutableMap.of(
			"sqlite", "create table if not exists '%s' (id TEXT PRIMARY KEY, value NUMERIC"+columnBuilder("NUMERIC")+", namecache TEXT, prefixcache TEXT, suffixcache TEXT, displaynamecache TEXT))",
			"h2", "create table if not exists '%s' ('id' VARCHAR(36) PRIMARY KEY, 'value' BIGINT"+columnBuilder("BIGINT")+", 'namecache' VARCHAR(16), 'prefixcache' VARCHAR(255), 'suffixcache' VARCHAR(255), 'displaynamecache' VARCHAR(255))",
			"mysql", "create table if not exists '%s' ('id' VARCHAR(36) PRIMARY KEY, 'value' BIGINT"+columnBuilder("BIGINT")+", 'namecache' VARCHAR(16), 'prefixcache' TINYTEXT, 'suffixcache' TINYTEXT, 'displaynamecache' TINYTEXT))"
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
		try {
			Connection conn = method.getConnection();
			String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
			PreparedStatement ps = conn.prepareStatement(String.format(
					method.formatStatement(SELECT_POSITION),
					tablePrefix+board,
					sortBy,
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

	private final Map<String, Integer> sortByIndexes = new HashMap<>();
	public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
		if(!plugin.getTopManager().boardExists(board)) {
			if(!nonExistantBoards.contains(board)) {
				nonExistantBoards.add(board);
			}
			return StatEntry.boardNotFound(plugin, -3, board, type);
		}
		StatEntry r = null;
		try {
			Connection conn = method.getConnection();
			String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
			PreparedStatement ps = conn.prepareStatement(String.format(
					method.formatStatement(SELECT_PLAYER),
					tablePrefix+board,
					sortBy
			));

			ResultSet rs = ps.executeQuery();
			int i = 0;
			while(rs.next()) {
				i++;
				String uuidraw = null;
				double value = -1;
				String name = "-Unknown-";
				String displayName = name;
				String prefix = "";
				String suffix = "";
				try {
					uuidraw = rs.getString(1);
					name = rs.getString(3);
					prefix = rs.getString(4);
					suffix = rs.getString(5);
					displayName = rs.getString(6);
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
				if(uuidraw == null) break;
				if(!player.getUniqueId().toString().equals(uuidraw)) continue;
				r = new StatEntry(plugin, i, board, prefix, name, displayName, UUID.fromString(uuidraw), suffix, value, type);
				break;
			}
			rs.close();
			method.close(conn);
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to get position/value of player:", e);
			return StatEntry.error(plugin, -1, board, type);
		}
		if(r == null) {
			return StatEntry.noData(plugin, -1, board, type);
		}
		return r;
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
			method.close(conn);
			plugin.getTopManager().fetchBoards();
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
			o.add(table.substring(tablePrefix.length()));
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
				if(player.getPlayer().hasPermission("ajleaderboards.dontupdate."+b)) continue;
			}
			updateStat(b, player);
		}
	}

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
			output = Double.parseDouble(convertPlaceholderOutput(outputraw));
		} catch(NumberFormatException e) {
			if(debug) Debug.info("Placeholder %"+board+"% for "+player.getName()+" returned a non-number! Ignoring it.");
			return;
		} catch(Exception e) {
			plugin.getLogger().log(Level.WARNING, "Placeholder %"+board+"% for player "+player.getName()+" threw an error:", e);
			return;
		}
		if(debug) Debug.info("Placeholder "+board+" for "+player.getName()+" returned "+output);

		String displayName = player.getName();
		if(player.isOnline()) {
			displayName = player.getPlayer().getDisplayName();
		}

		String prefix = "";
		String suffix = "";
		if(plugin.hasVault() && player instanceof Player) {
			prefix = plugin.getVaultChat().getPlayerPrefix((Player)player);
			suffix = plugin.getVaultChat().getPlayerSuffix((Player)player);
		}


		Map<TimedType, Double> lastTotals = new HashMap<>();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			lastTotals.put(type, getLastTotal(board, player, type));
		}


		if(debug) Debug.info("Updating "+player.getName()+" on board "+board+" with values v: "+output+" suffix: "+suffix+" prefix: "+prefix);
		try {
			Connection conn = method.getConnection();
			try {
				PreparedStatement statement = conn.prepareStatement(String.format(
						method.formatStatement(INSERT_PLAYER),
						tablePrefix+board
				));
				if(debug) Debug.info("in try");
				statement.setString(1, player.getUniqueId().toString());
				statement.setDouble(2, output);
				statement.setString(3, player.getName());
				statement.setString(4, prefix);
				statement.setString(5, suffix);
				statement.setString(6, displayName);
				int i = 6;
				for(TimedType type : TimedType.values()) {
					if(type == TimedType.ALLTIME) continue;
					long lastReset = plugin.getTopManager().getLastReset(board, type).get();
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
				if(debug) Debug.info("in catch");
				try(PreparedStatement statement = conn.prepareStatement(String.format(
						method.formatStatement(UPDATE_PLAYER),
						tablePrefix+board
				))) {
					statement.setDouble(1, output);
					statement.setString(2, player.getName());
					statement.setString(3, prefix);
					statement.setString(4, suffix);
					statement.setString(5, displayName);
					int i = 6;
					for(TimedType type : TimedType.values()) {
						if(type == TimedType.ALLTIME) continue;
						statement.setDouble(i++, output-lastTotals.get(type));
					}
					statement.setString(i, player.getUniqueId().toString());
					statement.executeUpdate();
					statement.close();
				}
				method.close(conn);
			}
			method.close(conn);
			if(!conn.isClosed() && method.requiresClose()) {
				plugin.getLogger().warning("Not closed!");
			}
		} catch(ExecutionException | InterruptedException | SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Unable to update stat for player:", e);
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
		long startTime = System.currentTimeMillis();
		LocalDateTime startDateTime = LocalDateTime.now();
		long newTime = startDateTime.atOffset(ZoneOffset.UTC).toEpochSecond()*1000;
		Debug.info(board+" "+type+" "+startDateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)+" "+newTime);
		if(type.equals(TimedType.ALLTIME)) {
			throw new IllegalArgumentException("Cannot reset ALLTIME!");
		}
		Debug.info("Resetting "+board+" "+type.lowerName()+" leaderboard");
		long lastReset = plugin.getTopManager().getLastReset(board, type).get()*1000L;
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

	Map<String, Integer> dataSortByIndexes = new HashMap<>();
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
		if(uuidRaw == null) {
			return StatEntry.noData(plugin, position, board, type);
		} else {
			return new StatEntry(plugin, position, board, prefix, name, displayName, UUID.fromString(uuidRaw), suffix, value, type);
		}
	}

	public List<String> getNonExistantBoards() {
		return nonExistantBoards;
	}
}
