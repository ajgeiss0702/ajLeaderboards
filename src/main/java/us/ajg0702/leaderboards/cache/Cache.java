package us.ajg0702.leaderboards.cache;

import me.clip.placeholderapi.PlaceholderAPI;
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
			ResultSet r = statement.executeQuery("select id,value,daily_delta,weekly_delta,monthly_delta,namecache,prefixcache,suffixcache from `"+tablePrefix+board+"` order by value desc limit "+(position-1)+","+position);
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
				switch(type) {
					case ALLTIME:
						value = r.getDouble("value");
						break;
					case DAILY:
						value = r.getDouble("daily_delta");
						break;
					case WEEKLY:
						value = r.getDouble("weekly_delta");
						break;
					case MONTHLY:
						value = r.getDouble("monthly_delta");
						break;
				}
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
		try {
			Connection conn = method.getConnection();
			Statement statement = conn.createStatement();
			if(method instanceof SqliteMethod) {
				statement.executeUpdate("create table if not exists `"+tablePrefix+name+"` (" +
						"id TEXT PRIMARY KEY, " +
						"value NUMERIC, " +
						"daily_delta NUMERIC, " +
						"daily_lasttotal NUMERIC, " +
						"daily_timestamp DATETIME, " +
						"weekly_delta NUMERIC, " +
						"weekly_lasttotal NUMERIC, " +
						"weekly_timestamp DATETIME, " +
						"monthly_delta NUMERIC, " +
						"monthly_lasttotal NUMERIC, " +
						"monthly_timestamp DATETIME, " +
						"namecache TEXT, " +
						"prefixcache TEXT, " +
						"suffixcache TEXT" +
						")");
			} else {
				statement.executeUpdate("create table if not exists \n" +
						"`"+tablePrefix+name+"`\n" +
						" (\n" +
						" id VARCHAR(36) PRIMARY KEY,\n" +
						" value BIGINT,\n" +
						" daily_delta BIGINT," +
						" daily_lasttotal BIGINT," +
						" daily_timestamp TIMESTAMP," +
						" weekly_delta BIGINT," +
						" weekly_lasttotal BIGINT," +
						" weekly_timestamp TIMESTAMP," +
						" monthly_delta BIGINT," +
						" monthly_lasttotal BIGINT," +
						" monthly_timestamp TIMESTAMP" +
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

	public void removePlayer(String board, String playerName) {
			try {
				Connection conn = method.getConnection();
				conn.createStatement().executeUpdate("delete from `"+tablePrefix+board+"` where namecache=`"+playerName+"`");
				method.close(conn);
			} catch (SQLException e) {
				plugin.getLogger().severe("Unable to remove player from board:");
				e.printStackTrace();
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
				if(b.indexOf(tablePrefix) != 0) continue;
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



		Debug.info("Updating "+player.getName()+" on board "+board+" with values v: "+output+" suffix: "+suffix+" prefix: "+prefix);
		String insertStatment = "insert into `"+tablePrefix+board+"` (id, value, namecache, prefixcache, suffixcache) values (?, ?, ?, ?, ?)";
		String updateStatement = "update `"+tablePrefix+board+"` set value="+output+", namecache=?, prefixcache=?, suffixcache=? where id=?";
		try {
			Connection conn = method.getConnection();
			try(PreparedStatement statement = conn.prepareStatement(insertStatment)) {
				Debug.info("in try");
				statement.setString(1, player.getUniqueId().toString());
				statement.setDouble(2, output);
				statement.setString(3, player.getName());
				statement.setString(4, prefix);
				statement.setString(5, suffix);
				statement.executeUpdate();
			} catch(SQLException e) {
				Debug.info("in catch");
				try(PreparedStatement statement = conn.prepareStatement(updateStatement)) {
					statement.setString(1, player.getName());
					statement.setString(2, prefix);
					statement.setString(3, suffix);
					statement.setString(4, player.getUniqueId().toString());
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
		String typeName =  type.toString().toLowerCase(Locale.ROOT);
		try(Connection conn = method.getConnection()) {
			ResultSet rs = conn.createStatement().executeQuery(
					"select "+typeName+"_lasttotal from "+tablePrefix+board+" where id='"+player.getUniqueId()+"'");
			last = rs.getInt(1);
		} catch(SQLException ignored) {}

		return last;
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
