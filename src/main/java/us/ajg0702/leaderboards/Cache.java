package us.ajg0702.leaderboards;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import us.ajg0702.leaderboards.boards.StatEntry;

public class Cache {
	static Cache INSTANCE;
	public static Cache getInstance() {
		return INSTANCE;
	}
	public static Cache getInstance(Main pl) {
		if(INSTANCE == null) {
			INSTANCE = new Cache(pl);
		}
		return INSTANCE;
	}
	
	public Main getPlugin() {
		return pl;
	}
	
	
	Main pl;
	Connection conn;
	private Cache(Main pl) {
		this.pl = pl;
		
		pl.getDataFolder().mkdirs();
		
		init(true);
		
	}
	
	private void init(boolean retry) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String url = "jdbc:sqlite:"+pl.getDataFolder().getAbsolutePath()+File.separator+"cache.db";
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) {
			if(retry && e.getMessage().indexOf("No suitable driver found for jdbc:sqlite:") != -1) {
				pl.getLogger().info("Downloading sqlite drivers..");
				Downloader.getInstance().downloadAndLoad();
				init(false);
				return;
			}
			pl.getLogger().severe("Unnable to create cache file! The plugin will not work correctly!");
			e.printStackTrace();
			return;
		}
		 try(Statement statement = conn.createStatement()) {
			 ResultSet rs = statement.executeQuery("PRAGMA user_version;");
             int version = rs.getInt(1);
             rs.close();
             
             if(version == 0) {
            	 pl.getLogger().info("Running table updater. (pv"+version+")");
            	 for(String b : getBoards()) {
            		 statement.executeUpdate("alter table '"+b+"' add column namecache TEXT;");
            		 statement.executeUpdate("alter table '"+b+"' add column prefixcache TEXT;");
            		 statement.executeUpdate("alter table '"+b+"' add column suffixcache TEXT;");
            	 }
            	 statement.executeUpdate("PRAGMA user_version = 1;");
             }
             statement.close();
         } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	StatEntry lastStatEntry = null;
	
	public StatEntry getStat(int position, String board) {
		if(lastStatEntry != null && lastStatEntry.getBoard().equals(board) && lastStatEntry.getPosition() == position) {
			return lastStatEntry;
		}
		if(!boardExists(board)) {
			StatEntry se = new StatEntry(position, board, "", "Board does not exist", "", 0);
			lastStatEntry = se;
			return se;
		}
		try {
			Statement statement = conn.createStatement();
			ResultSet r = statement.executeQuery("select id,value,namecache,prefixcache,suffixcache from '"+board+"' order by value desc limit "+(position-1)+","+position);
			String uuidraw = null;
			double value = -1;
			String name = "-Unknown-";
			String prefix = "";
			String suffix = "";
			try {
				uuidraw = r.getString("id");
				value = r.getDouble("value");
				name = r.getString("namecache");
				prefix = r.getString("prefixcache");
				suffix = r.getString("suffixcache");
				
			} catch(SQLException e) {
				if(e.getMessage().indexOf("ResultSet closed") == -1) {
					throw e;
				}
			}
			r.close();
			statement.close();
			if(name == null) name = "-Unknown";
			if(uuidraw == null) {
				StatEntry se = new StatEntry(position, board, "", pl.config.getString("no-data-name"), "", 0);
				lastStatEntry = se;
				return se;
			} else {
				StatEntry se = new StatEntry(position, board, prefix, name, suffix, value);
				lastStatEntry = se;
				return se;
			}
		} catch(SQLException e) {
			pl.getLogger().severe("Unable to stat of player:");
			e.printStackTrace();
			return new StatEntry(position, board, "", "An error occured", "", 0);
		}
	}
	
	public int getPlace(OfflinePlayer player, String board) {
		List<String> l = new ArrayList<>();
        try {
        	Statement statement = conn.createStatement();
            ResultSet r = statement.executeQuery("select id,value from '" + board + "' order by value desc");
            while (r.next()) {
                l.add(r.getString(1));
            }
            r.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return l.indexOf(player.getUniqueId().toString()) + 1;
	}
	
	public boolean createBoard(String name) {
		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate("create table if not exists '"+name+"' (id TEXT PRIMARY KEY, value NUMERIC, namecache TEXT, prefixcache TEXT, suffixcache TEXT)");
			statement.close();
			return true;
		} catch (SQLException e) {
			pl.getLogger().severe("Unable to create board:");
			e.printStackTrace();
			return false;
		}
	}
	
	
	public boolean boardExists(String board) {
		return getBoards().contains(board);
	}
	
	public List<String> getBoards() {
		List<String> o = new ArrayList<>();
		ResultSet r;
		try {
			Statement statement = conn.createStatement();
			r = statement.executeQuery("SELECT \n" + 
					"    name\n" + 
					"FROM \n" + 
					"    sqlite_master \n" + 
					"WHERE \n" + 
					"    type ='table' AND \n" + 
					"    name NOT LIKE 'sqlite_%';");
			while(r.next()) {
				o.add(r.getString(1));
			}
			statement.close();
			r.close();
		} catch (SQLException e) {
			pl.getLogger().severe("Unable to get list of tables:");
			e.printStackTrace();
		}
		return o;
	}
	
	public boolean removeBoard(String board) {
		if(!getBoards().contains(board)) return true;
		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate("drop table '"+board+"';");
			statement.close();
			return true;
		} catch (SQLException e) {
			pl.getLogger().warning("An error occurred while trying to remove a board:");
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
		Double output;
		try {
			outputraw = PlaceholderAPI.setPlaceholders(player, "%"+board+"%");
			output = Double.valueOf(outputraw);
		} catch(NumberFormatException e) {
			return;
		} catch(Exception e) {
			pl.getLogger().warning("Placeholder %"+board+"% for player "+player.getName()+" threw an error:");
			e.printStackTrace();
			return;
		}
		//pl.getLogger().info("Placeholder "+board+" for "+player.getName()+" returned "+output);
		String prefix = "";
		String suffix = "";
		if(pl.vault && player instanceof Player) {
			prefix = pl.vaultChat.getPlayerPrefix((Player)player);
			suffix = pl.vaultChat.getPlayerSuffix((Player)player);
		}
		try {
			try {
				PreparedStatement statement = conn.prepareStatement("insert into '"+board+"' (id, value, namecache, prefixcache, suffixcache) values (?, ?, ?, ?, ?)");
				statement.setString(1, player.getUniqueId().toString());
				statement.setDouble(2, output);
				statement.setString(3, player.getName());
				statement.setString(4, prefix);
				statement.setString(5, suffix);
				statement.executeUpdate();
				statement.close();
			} catch(SQLException e) {
				PreparedStatement statement = conn.prepareStatement("update '"+board+"' set value="+output+", namecache='"+player.getName()+"', prefixcache=?, suffixcache=? where id='"+player.getUniqueId()+"'");
				statement.setString(1, prefix);
				statement.setString(2, suffix);
				statement.executeUpdate();
				statement.close();
			}
		} catch(SQLException e) {
			pl.getLogger().severe("Unable to update stat for player:");
			e.printStackTrace();
		}
		
		
	}
}
