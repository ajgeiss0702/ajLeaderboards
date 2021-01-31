package us.ajg0702.leaderboards.signs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import us.ajg0702.leaderboards.Cache;
import us.ajg0702.leaderboards.Main;
import us.ajg0702.leaderboards.armorstands.ArmorStandManager;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.utils.spigot.Messages;

public class SignManager {
	static SignManager instance;
	public static SignManager getInstance() {
		return instance;
	}
	public static SignManager getInstance(Main pl) {
		if(instance == null) {
			instance = new SignManager(pl);
		}
		return instance;
	}
	
	Main pl;
	
	public YamlConfiguration cfg;
	File cfgFile;
	
	Messages msgs;
	
	private SignManager(Main pl) {
		this.pl = pl;
		msgs = Messages.getInstance();
		reload();
		
		Bukkit.getScheduler().runTaskTimerAsynchronously(pl, new Runnable() {
			public void run() {
				updateSigns();
			}
		}, 10*20, 20);
	}
	
	List<BoardSign> signs = new ArrayList<>();
	public void reload() {
		cfgFile = new File(pl.getDataFolder(), "displays.yml");
		cfg = YamlConfiguration.loadConfiguration(cfgFile);
		cfg.options().header("This file is for storing sign location, npcs, and other things in the plugin that might display data");
		
		signs.clear();
		if(cfg.contains("signs")) {
			List<String> rawsigns = cfg.getStringList("signs");
			for(String s : rawsigns) {
				try {
					signs.add(BoardSign.deserialize(s));
				} catch(Exception e) {
					pl.getLogger().warning("An error occurred while loading a sign:");
					e.printStackTrace();
				}
			}
		}
		updateNameCache();
	}
	
	public List<BoardSign> getSigns() {
		return signs;
	}
	
	public boolean removeSign(Location l) {
		boolean save = false;
		Iterator<BoardSign> i = signs.iterator();
		while(i.hasNext()) {
			BoardSign s = i.next();
			if(l.equals(s.getLocation())) {
				i.remove();
				save = true;
				s.setText("", "", "", "");
				break;
			}
		}
		if(save) saveFile();
		return save;
	}
	
	
	public void addSign(Location loc, String board, int pos) {
		signs.add(new BoardSign(loc, board, pos));
		saveFile();
	}
	
	public void saveFile() {
		List<String> signsraw = new ArrayList<>();
		for(BoardSign sign : signs) {
			signsraw.add(sign.serialize());
		}
		cfg.set("signs", signsraw);
		try {
			cfg.save(cfgFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void updateSigns() {
		updateNameCache();
		for(BoardSign sign : signs) {
			updateSign(sign);
		}
	}
	
	HashMap<String, String> names = new HashMap<>();
	public void updateNameCache() {
		List<String> namesraw = pl.getAConfig().getStringList("value-names");
		for(String s : namesraw) {
			if(!s.contains("%")) continue;
			String[] parts = s.split("%");
			names.put(parts[0], parts[1]);
		}
	}
	
	public void updateSign(BoardSign sign) {
		
		String name = "";
		if(names.containsKey(sign.getBoard())) {
			name = names.get(sign.getBoard());
		}
		
		StatEntry r = Cache.getInstance().getStat(sign.getPosition(), sign.getBoard());
		
		List<String> lines = Arrays.asList(
				msgs.get("signs.top.1"), 
				msgs.get("signs.top.2"), 
				msgs.get("signs.top.3"), 
				msgs.get("signs.top.4"));
		List<String> plines = new ArrayList<>();
		for(String l : lines) {
			String pline = l
					.replaceAll("\\{POSITION\\}", sign.getPosition()+"")
					.replaceAll("\\{NAME\\}", r.getPlayer())
					.replaceAll("\\{VALUE\\}", r.getScorePretty())
					.replaceAll("\\{VALUENAME\\}", name)
					;
			plines.add(pline);
			
		}
		lines = null;
		@SuppressWarnings("deprecation")
		OfflinePlayer op = Bukkit.getOfflinePlayer(r.getPlayer());
		Bukkit.getScheduler().runTask(pl, new Runnable() {
			public void run() {
				if(op != null) {
					ArmorStandManager.getInstance().search(sign, op);
				}
				sign.setText(plines.get(0), plines.get(1), plines.get(2), plines.get(3));
			}
		});
		
		
	}
	
}
