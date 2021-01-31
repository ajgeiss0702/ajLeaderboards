package us.ajg0702.leaderboards;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
				Downloader.getInstance().downloadAndLoad();
				init(false);
				return;
			}
			pl.getLogger().severe("Unnable to create cache file! The plugin will not work correctly!");
			e.printStackTrace();
			return;
		}
		
		 try(ResultSet rs = conn.createStatement().executeQuery("PRAGMA user_version;")) {
             int version = rs.getInt(1);
             rs.close();
             
             if(version == 0) {
            	 pl.getLogger().info("Running table updater. (pv"+version+")");
            	 for(String b : getBoards()) {
            		 conn.createStatement().executeUpdate("alter table '"+b+"' add column namecache TEXT;");
            		 conn.createStatement().executeUpdate("alter table '"+b+"' add column prefixcache TEXT;");
            		 conn.createStatement().executeUpdate("alter table '"+b+"' add column suffixcache TEXT;");
            	 }
            	 conn.createStatement().executeUpdate("PRAGMA user_version = 1;");
             }
         } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	public StatEntry getStat(int position, String board) {
		if(!boardExists(board)) {
			return new StatEntry("", "Board does not exist", "", 0);
		}
		try {
			ResultSet r = conn.createStatement().executeQuery("select id,value,namecache,prefixcache,suffixcache from '"+board+"' order by value desc limit "+(position-1)+","+position);
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
			if(name == null) name = "-Unknown";
			if(uuidraw == null) return new StatEntry("", pl.config.getString("no-data-name"), "", 0);
			OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(uuidraw));
			return new StatEntry(prefix, p.getName() == null ? name : p.getName(), suffix, value);
		} catch(SQLException e) {
			pl.getLogger().severe("Unable to stat of player:");
			e.printStackTrace();
			return new StatEntry("", "An error occured", "", 0);
		}
	}
	
	public int getPlace(OfflinePlayer player, String board) {
		final List<String> l = new ArrayList<>();
        try {
            final ResultSet r = conn.createStatement().executeQuery("select id,value from '" + board + "' order by value desc");
            while (r.next()) {
                l.add(r.getString(1));
            }
            r.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return l.indexOf(player.getUniqueId().toString()) + 1;
	}
	
	public boolean createBoard(String name) {
		try {
			conn.createStatement().executeUpdate("create table if not exists '"+name+"' (id TEXT PRIMARY KEY, value NUMERIC, namecache TEXT, prefixcache TEXT, suffixcache TEXT)");
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
			r = conn.createStatement().executeQuery("SELECT \n" + 
					"    name\n" + 
					"FROM \n" + 
					"    sqlite_master \n" + 
					"WHERE \n" + 
					"    type ='table' AND \n" + 
					"    name NOT LIKE 'sqlite_%';");
		
			while(r.next()) {
				o.add(r.getString(1));
			}
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
			conn.createStatement().executeUpdate("drop table '"+board+"';");
			return true;
		} catch (SQLException e) {
			pl.getLogger().warning("An error occurred while trying to remove a board:");
			e.printStackTrace();
			return false;
		}
	}
	
	
	public void updatePlayerStats(Player player) {
		for(String b : getBoards()) {
			updateStat(b, player);
		}
	}
	
	public void updateStat(String board, Player player) {
		String outputraw = PlaceholderAPI.setPlaceholders(player, "%"+board+"%");
		Double output;
		try {
			output = Double.valueOf(outputraw);
		} catch(NumberFormatException e) {
			return;
		} catch(Exception e) {
			pl.getLogger().warning("Placeholder %"+board+"% for player "+player.getName()+" threw an error:");
			e.printStackTrace();
			return;
		}
		String prefix = "";
		String suffix = "";
		if(pl.vault) {
			prefix = pl.vaultChat.getPlayerPrefix(player);
			suffix = pl.vaultChat.getPlayerSuffix(player);
		}
		try {
			
			try {
				conn.createStatement().executeUpdate("insert into '"+board+"' (id, value, namecache, prefixcache, suffixcache) values ('"+player.getUniqueId()+"', "+output+", '"+player.getName()+"', '"+prefix+"', '"+suffix+"')");
			} catch(SQLException e) {
				conn.createStatement().executeUpdate("update '"+board+"' set value="+output+", namecache='"+player.getName()+"', prefixcache='"+prefix+"', suffixcache='"+suffix+"' where id='"+player.getUniqueId()+"'");
			}
		} catch(SQLException e) {
			pl.getLogger().severe("Unable to update stat for player:");
			e.printStackTrace();
		}
		
		
	}
}
