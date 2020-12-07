package us.ajg0702.leaderboards;

import java.util.LinkedHashMap;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.chat.Chat;
import us.ajg0702.leaderboards.signs.SignManager;
import us.ajg0702.utils.spigot.Config;
import us.ajg0702.utils.spigot.Messages;

public class Main extends JavaPlugin {
	
	Config config;
	Metrics metrics;
	
	Placeholders placeholders;
	Commands commands;
	
	Messages msgs;
	
	boolean vault = false;
	Chat vaultChat;
	
	@Override
	public void onEnable() {
		Downloader.getInstance(this);
		Cache cache = Cache.getInstance(this);
		
		commands = new Commands(this);
		getCommand("ajleaderboards").setExecutor(commands);
		getCommand("ajleaderboards").setTabCompleter(commands);
		
		config = new Config(this);
		
		placeholders = new Placeholders(this);
		placeholders.register();
		
		
		LinkedHashMap<String, String> dmsgs = new LinkedHashMap<>();
		
		dmsgs.put("signs.top.1", "&7&m       &r #{POSITION} &7&m       ");
		dmsgs.put("signs.top.2", "&6{NAME}");
		dmsgs.put("signs.top.3", "&e{VALUE} {VALUENAME}");
		dmsgs.put("signs.top.4", "&7&m                   ");
		
		
		msgs = Messages.getInstance(this, dmsgs);
		
		
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()) {
					cache.updatePlayerStats(p);
				}
			}
		}, 30*20, 60*20);
		
		metrics = new Metrics(this, 9338);
		
		Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
		
		SignManager.getInstance(this);
		
		Bukkit.getScheduler().runTask(this, new Runnable() {
			public void run() {
				if(Bukkit.getPluginManager().isPluginEnabled("Vault")) {
					RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
			        if(rsp == null) {
			        	vault = false;
			        	getLogger().warning("Vault prefix hook failed! Make sure you have a plugin that implements chat (e.g. Luckperms)");
			        } else {
			        	vaultChat = rsp.getProvider();
				        vault = vaultChat != null;	
			        }
				}
			}
		});
		
		getLogger().info("Plugin enabled! "+Cache.getInstance().getBoards().size()+" leaderboards loaded.");
	}
	
	public Config getAConfig() {
		return config;
	}
}
