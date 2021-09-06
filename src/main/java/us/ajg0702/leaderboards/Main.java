package us.ajg0702.leaderboards;

import net.milkbowl.vault.chat.Chat;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.heads.HeadManager;
import us.ajg0702.leaderboards.signs.SignManager;
import us.ajg0702.utils.spigot.Config;
import us.ajg0702.utils.spigot.Messages;

import java.util.LinkedHashMap;

public class Main extends JavaPlugin {
	
	Config config;
	Metrics metrics;

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

		Placeholders placeholders = new Placeholders(this);
		placeholders.register();

		TopManager.getInstance(this);
		
		
		LinkedHashMap<String, String> dmsgs = new LinkedHashMap<>();
		
		dmsgs.put("signs.top.1", "&7&m       &r #{POSITION} &7&m       ");
		dmsgs.put("signs.top.2", "&6{NAME}");
		dmsgs.put("signs.top.3", "&e{VALUE} {VALUENAME}");
		dmsgs.put("signs.top.4", "&7&m                   ");
		
		
		msgs = Messages.getInstance(this, dmsgs);
		
		HeadManager.getInstance(this);
		
		
		reloadInterval();
		
		metrics = new Metrics(this, 9338);
		
		Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
		
		SignManager.getInstance(this);
		
		Bukkit.getScheduler().runTask(this, () -> {
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
		});
		
		getLogger().info("Plugin enabled! "+ Cache.getInstance().getBoards().size()+" leaderboards loaded.");
	}
	
	int updateTaskId = -1;
	public void reloadInterval() {
		if(updateTaskId != -1) {
			try {
				Bukkit.getScheduler().cancelTask(updateTaskId);
			} catch(IllegalArgumentException ignored) {}
			updateTaskId = -1;
		}
		updateTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			for(Player p : Bukkit.getOnlinePlayers()) {
				Cache.getInstance().updatePlayerStats(p);
			}
		}, 10*20, config.getInt("stat-refresh")).getTaskId();
	}
	
	public Config getAConfig() {
		return config;
	}

	public boolean hasVault() {
		return vault;
	}

	public Chat getVaultChat() {
		return vaultChat;
	}
}
