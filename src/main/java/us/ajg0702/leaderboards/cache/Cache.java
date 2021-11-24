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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
			switch(type) {
				case ALLTIME:
					Connection conn = method.getConnection();
					Statement statement = conn.createStatement();
					ResultSet r = statement.executeQuery("select id,value,namecache,prefixcache,suffixcache from `"+tablePrefix+board+"` order by value desc limit "+(position-1)+","+position);
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
						value = r.getDouble("value");
						name = r.getString("namecache");
						prefix = r.getString("prefixcache");
						suffix = r.getString("suffixcache");

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
					break;
				case DAILY:
					break;
				case WEEKLY:
					break;
				case MONTHLY:
					break;
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
				statement.executeUpdate("create table if not exists `"+tablePrefix+name+"` (id TEXT PRIMARY KEY, value NUMERIC, namecache TEXT, prefixcache TEXT, suffixcache TEXT)");
			} else {
				statement.executeUpdate("create table if not exists \n`"+tablePrefix+name+"`\n (\nid VARCHAR(36) PRIMARY KEY,\n value BIGINT,\n namecache VARCHAR(16),\n prefixcache TINYTEXT, suffixcache TINYTEXT)");

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

	public void removePlayer(String board, UUID player) {
			try {
				Connection conn = method.getConnection();
				conn.createStatement().executeUpdate("delete from `"+tablePrefix+board+"` where id=`"+player+"`");
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
		ResultSet r;
		try {
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
				String b = r.getString(1);
				if(b.indexOf(tablePrefix) != 0) continue;
				o.add(b.substring(tablePrefix.length()));
			}
			statement.close();
			r.close();
			method.close(conn);
		} catch (SQLException e) {
			plugin.getLogger().severe("Unable to get list of tables:");
			e.printStackTrace();
		}
		return o;
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
			outputraw = PlaceholderAPI.setPlaceholders(player, "%"+alternatePlaceholders(board)+"%").replaceAll(",", "");
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
		try {
			Connection conn = method.getConnection();
			try(PreparedStatement statement = conn.prepareStatement("insert into `"+tablePrefix+board+"` (id, value, namecache, prefixcache, suffixcache) values (?, ?, ?, ?, ?)");) {
				Debug.info("in try");
				statement.setString(1, player.getUniqueId().toString());
				statement.setDouble(2, output);
				statement.setString(3, player.getName());
				statement.setString(4, prefix);
				statement.setString(5, suffix);
				statement.executeUpdate();
			} catch(SQLException e) {
				Debug.info("in catch");
				try(PreparedStatement statement = conn.prepareStatement("update `"+tablePrefix+board+"` set value="+output+", namecache=?, prefixcache=?, suffixcache=? where id=?")) {
					statement.setString(2, prefix);
					statement.setString(3, suffix);
					statement.setString(1, player.getName());
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

	private static final HashMap<String, String> altPlaceholders = new HashMap<String, String>() {{
		put("ajpk_stats_highscore", "ajpk_stats_highscore_nocache");
		put("ajtr_stats_wins", "ajtr_stats_wins_nocache");
		put("ajtr_stats_losses", "ajtr_stats_losses_nocache");
		put("ajtr_stats_gamesplayed", "ajtr_stats_gamesplayer_nocache");
	}};
	public static String alternatePlaceholders(String board) {
		if(!altPlaceholders.containsKey(board)) return board;
		return altPlaceholders.get(board);
	}
}
