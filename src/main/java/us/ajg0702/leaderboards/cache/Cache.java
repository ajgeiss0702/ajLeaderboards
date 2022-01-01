package us.ajg0702.leaderboards.cache;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.methods.MysqlMethod;
import us.ajg0702.leaderboards.cache.methods.SqliteMethod;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.*;
import java.util.*;

public class Cache {
	
	public LeaderboardPlugin getPlugin() {
		return plugin;
	}

	ConfigFile storageConfig;
	LeaderboardPlugin plugin;
	CacheMethod method;

	String tablePrefix;

	public Cache(LeaderboardPlugin plugin) {
		this.plugin = plugin;

		plugin.getDataFolder().mkdirs();

		try {
			storageConfig = new ConfigFile(plugin.getDataFolder(), plugin.getLogger(), "cache_storage.yml");
		} catch (ConfigurateException e) {
			e.printStackTrace();
		}

		if(storageConfig.getString("method").equalsIgnoreCase("mysql")) {
			plugin.getLogger().info("Using MySQL for board cache. ("+storageConfig.getString("method")+")");
			method = new MysqlMethod();
			tablePrefix = storageConfig.getString("table_prefix");
		} else {
			plugin.getLogger().info("Using SQLite for board cache. ("+storageConfig.getString("method")+")");
			method = new SqliteMethod();
			tablePrefix = "";
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
		if(!boardExists(board)) {
			return new StatEntry(plugin, position, board, "", "Board does not exist", null, "", 0, type);
		}
		try {
			Connection conn = method.getConnection();
			Statement statement = conn.createStatement();
			StringBuilder deltaBuilder = new StringBuilder();
			for(TimedType t : TimedType.values()) {
				if(t == TimedType.ALLTIME) continue;
				deltaBuilder.append(t.lowerName()).append("_delta,");
			}
			String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
			ResultSet r = statement.executeQuery("select id,value,"+deltaBuilder+"namecache,prefixcache,suffixcache from `"+tablePrefix+board+"` order by "+sortBy+" desc limit "+(position-1)+","+position);
			String uuidraw = null;
			double value = -1;
			String name = "-Unknown-";
			String prefix = "";
			String suffix = "";
			if(method instanceof MysqlMethod) {
				r.next();
			}
			try {
				uuidraw = r.getString("id");
				name = r.getString("namecache");
				prefix = r.getString("prefixcache");
				suffix = r.getString("suffixcache");
				value = r.getDouble(sortBy);
			} catch(SQLException e) {
				if(
						!e.getMessage().contains("ResultSet closed") &&
								!e.getMessage().contains("empty result set")
				) {
					throw e;
				}
			}
			r.close();
			statement.close();
			method.close(conn);
			if(name == null) name = "-Unknown";
			if(uuidraw == null) {
				return new StatEntry(plugin, position, board, "", plugin.getAConfig().getString("no-data-name"), null, "", 0, type);
			} else {
				return new StatEntry(plugin, position, board, prefix, name, UUID.fromString(uuidraw), suffix, value, type);
			}
		} catch(SQLException e) {
			plugin.getLogger().severe("Unable to get stat of player:");
			e.printStackTrace();
			return new StatEntry(plugin, position, board, "", "An error occured", null, "", 0, type);
		}
	}

	public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
		StatEntry r = null;
		int i = 1;
		while(i < 10000000 && r == null) {
			StatEntry rt = plugin.getTopManager().getStat(i, board, type);
			i++;
			if(rt.getPlayerID() == null || player.getUniqueId().equals(rt.getPlayerID())) {
				r = rt;
			}
		}
		return r;
	}

	public boolean createBoard(String name) {
		String t = method instanceof SqliteMethod ? "NUMERIC" : "BIGINT";
		StringBuilder columns = new StringBuilder();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			columns
					.append(",\n").append(type.name().toLowerCase(Locale.ROOT)).append("_delta ").append(t)
					.append(",\n").append(type.name().toLowerCase(Locale.ROOT)).append("_lasttotal ").append(t)
					.append(",\n").append(type.name().toLowerCase(Locale.ROOT)).append("_timestamp ").append(t);
		}
		try {
			Connection conn = method.getConnection();
			Statement statement = conn.createStatement();
			if(method instanceof SqliteMethod) {
				statement.executeUpdate("create table if not exists `"+tablePrefix+name+"` (" +
						"id TEXT PRIMARY KEY, " +
						"value NUMERIC"+ columns +", " +
						"namecache TEXT, " +
						"prefixcache TEXT, " +
						"suffixcache TEXT" +
						")");
			} else {
				statement.executeUpdate("create table if not exists \n" +
						"`"+tablePrefix+name+"`\n" +
						" (\n" +
						" id VARCHAR(36) PRIMARY KEY,\n" +
						" value BIGINT"+ columns +",\n" +
						" namecache VARCHAR(16)," +
						" prefixcache TINYTEXT," +
						" suffixcache TINYTEXT" +
						")");

			}
			statement.close();
			method.close(conn);
			return true;
		} catch (SQLException e) {
			plugin.getLogger().severe("Unable to create board:");
			e.printStackTrace();
			return false;
		}
	}

	public boolean removePlayer(String board, String playerName) {
			try {
				Connection conn = method.getConnection();
				conn.createStatement().executeUpdate("delete from `"+tablePrefix+board+"` where namecache=`"+playerName+"`");
				method.close(conn);
				return true;
			} catch (SQLException e) {
				plugin.getLogger().severe("Unable to remove player from board:");
				e.printStackTrace();
				return false;
			}
	}
	

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
			if(method instanceof SqliteMethod) {
				r = statement.executeQuery("SELECT \n" +
						"    name\n" +
						"FROM \n" +
						"    sqlite_master \n" +
						"WHERE \n" +
						"    type ='table' AND \n" +
						"    name NOT LIKE 'sqlite_%';");
			} else {
				r = statement.executeQuery("show tables;");
			}
			while(r.next()) {
				String e = r.getString(1);
				if(e.indexOf(tablePrefix) != 0) continue;
				b.add(e);
			}

			statement.close();
			r.close();
			method.close(conn);
		} catch(SQLException e) {
			plugin.getLogger().severe("Unable to get list of tables:");
			e.printStackTrace();
		}
		return b;
	}

	public boolean removeBoard(String board) {
		if(!getBoards().contains(board)) return true;
		try {
			if(method instanceof SqliteMethod) {
				((SqliteMethod) method).newConnection();
			}
			Connection conn = method.getConnection();
			Statement statement = conn.createStatement();
			statement.executeUpdate("drop table `"+tablePrefix+board+"`;");
			statement.close();
			method.close(conn);
			return true;
		} catch (SQLException e) {
			plugin.getLogger().warning("An error occurred while trying to remove a board:");
			e.printStackTrace();
			return false;
		}
	}
	

	public void updatePlayerStats(OfflinePlayer player) {
		for(String b : getBoards()) {
			if(player.isOnline() && player.getPlayer() != null) {
				if(player.getPlayer().hasPermission("ajleaderboards.dontupdate."+b)) return;
			}
			updateStat(b, player);
		}
	}

	public void updateStat(String board, OfflinePlayer player) {
		String outputraw;
		double output;
		try {
			outputraw = PlaceholderAPI.setPlaceholders(player, "%"+alternatePlaceholders(board)+"%")
					.replaceAll(",", "");
			output = Double.parseDouble(outputraw);
		} catch(NumberFormatException e) {
			return;
		} catch(Exception e) {
			plugin.getLogger().warning("Placeholder %"+board+"% for player "+player.getName()+" threw an error:");
			e.printStackTrace();
			return;
		}
		Debug.info("Placeholder "+board+" for "+player.getName()+" returned "+output);

		String prefix = "";
		String suffix = "";
		if(plugin.hasVault() && player instanceof Player) {
			prefix = plugin.getVaultChat().getPlayerPrefix((Player)player);
			suffix = plugin.getVaultChat().getPlayerSuffix((Player)player);
		}


		StringBuilder addTables = new StringBuilder();
		StringBuilder addQs = new StringBuilder();
		StringBuilder addUpdates = new StringBuilder();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			String name = type.name().toLowerCase(Locale.ROOT);
			addTables
					.append(", ").append(name).append("_delta")
					.append(", ").append(name).append("_lasttotal")
					.append(", ").append(name).append("_timestamp");
			addQs.append(", ?").append(", ?").append(", ?");

			addUpdates
					.append(", ").append(name).append("_delta=?");
		}

		Map<TimedType, Double> lastTotals = new HashMap<>();
		for(TimedType type : TimedType.values()) {
			if(type == TimedType.ALLTIME) continue;
			lastTotals.put(type, getLastTotal(board, player, type));
		}


		Debug.info("Updating "+player.getName()+" on board "+board+" with values v: "+output+" suffix: "+suffix+" prefix: "+prefix);
		String insertStatment = "insert into '"+tablePrefix+board+"' (id, value, namecache, prefixcache, suffixcache"+ addTables +") values (?, ?, ?, ?, ?"+ addQs +")";
		String updateStatement = "update '"+tablePrefix+board+"' set value="+output+", namecache=?, prefixcache=?, suffixcache=?"+ addUpdates +" where id=?";
		try {
			Connection conn = method.getConnection();
			try(PreparedStatement statement = conn.prepareStatement(insertStatment)) {
				Debug.info("in try");
				statement.setString(1, player.getUniqueId().toString());
				statement.setDouble(2, output);
				statement.setString(3, player.getName());
				statement.setString(4, prefix);
				statement.setString(5, suffix);
				int i = 5;
				for(TimedType type : TimedType.values()) {
					if(type == TimedType.ALLTIME) continue;
					long lastReset = getLastReset(board, type);
					Debug.info(board+" "+type+" lastReset: "+lastReset);
					statement.setDouble(++i, 0);
					statement.setDouble(++i, output);
					statement.setLong(++i, lastReset == 0 ? System.currentTimeMillis() : lastReset);
				}

				statement.executeUpdate();
			} catch(SQLException e) {
				Debug.info("in catch");
				try(PreparedStatement statement = conn.prepareStatement(updateStatement)) {
					statement.setString(1, player.getName());
					statement.setString(2, prefix);
					statement.setString(3, suffix);
					int i = 4;
					for(TimedType type : TimedType.values()) {
						if(type == TimedType.ALLTIME) continue;
						statement.setDouble(i++, output-lastTotals.get(type));
					}
					statement.setString(i++, player.getUniqueId().toString());
					statement.executeUpdate();
				}

			}
			method.close(conn);
		} catch(SQLException e) {
			plugin.getLogger().severe("Unable to update stat for player:");
			e.printStackTrace();
		}
	}


	public double getLastTotal(String board, OfflinePlayer player, TimedType type) {
		double last = 0;
		try {
			Connection conn = method.getConnection();
			ResultSet rs = conn.createStatement().executeQuery(
					"select "+type.lowerName()+"_lasttotal from "+tablePrefix+board+" where id='"+player.getUniqueId()+"'");
			last = rs.getInt(1);
			method.close(conn);
		} catch(SQLException ignored) {}

		return last;
	}

	public long getLastReset(String board, TimedType type) {
		long last = 0;
		try {
			Connection conn = method.getConnection();
			ResultSet rs = conn.createStatement().executeQuery(
					"select "+type.lowerName()+"_timestamp from "+tablePrefix+board+" limit 1");
			last = rs.getLong(type.lowerName()+"_timestamp");
			method.close(conn);
		} catch(SQLException ignored) {}

		return last;
	}

	public void reset(String board, TimedType type) {
		long startTime = System.currentTimeMillis();
		if(type.equals(TimedType.ALLTIME)) {
			throw new IllegalArgumentException("Cannot reset ALLTIME!");
		}
		plugin.getLogger().info("Resetting "+board+" "+type.lowerName()+" leaderboard");
		String t = type.lowerName();
		try {
			Connection conn = method.getConnection();
			ResultSet rs = conn.createStatement().executeQuery("select id,value from "+tablePrefix+board+";");
			Map<String, Double> uuids = new HashMap<>();
			while(rs.next()) {
				uuids.put(rs.getString("id"), rs.getDouble("value"));
			}
			rs.close();
			for(String idRaw : uuids.keySet()) {
				try {
					Connection con = method instanceof SqliteMethod ? conn : method.getConnection();
					String update = "update "+tablePrefix+board+" set "+t+"_lasttotal="+uuids.get(idRaw)+", "+t+"_delta=0, "+t+"_timestamp="+System.currentTimeMillis()+" where id='"+idRaw+"'";
					con.createStatement().executeUpdate(update);
					Debug.info(update);
					method.close(con);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			method.close(conn);
		} catch (SQLException e) {
			plugin.getLogger().severe("An error occurred while resetting a timed leaderboard:");
			e.printStackTrace();
		}
		plugin.getLogger().info("Reset of "+board+" "+type.lowerName()+" took "+(System.currentTimeMillis()-startTime)+"ms");
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
}
