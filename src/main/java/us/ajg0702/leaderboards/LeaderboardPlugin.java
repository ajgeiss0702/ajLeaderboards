package us.ajg0702.leaderboards;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.commands.platforms.bukkit.BukkitCommand;
import us.ajg0702.commands.platforms.bukkit.BukkitSender;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.commands.main.MainCommand;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;

import java.util.LinkedHashMap;

public class LeaderboardPlugin extends JavaPlugin {

    private Config config;
    private Cache cache;
    private Messages messages;
    private TopManager topManager;

    @Override
    public void onEnable() {

        BukkitCommand bukkitMainCommand = new BukkitCommand(new MainCommand(this));
        getCommand("ajleaderboards").setExecutor(bukkitMainCommand);
        getCommand("ajleaderboards").setTabCompleter(bukkitMainCommand);

        BukkitSender.setAdventure(this);

        try {
            config = new Config(getDataFolder(), getLogger());
        } catch (ConfigurateException e) {
            getLogger().severe("An error occurred while loading your config:");
            e.printStackTrace();
        }

        Debug.setLogger(getLogger());
        Debug.setDebug(config.getBoolean("debug"));

        LinkedHashMap<String, String> dmsgs = new LinkedHashMap<>();

        dmsgs.put("signs.top.1", "&7&m       &r #{POSITION} &7&m       ");
        dmsgs.put("signs.top.2", "&6{NAME}");
        dmsgs.put("signs.top.3", "&e{VALUE} {VALUENAME}");
        dmsgs.put("signs.top.4", "&7&m                   ");

        dmsgs.put("formatted.k", "k");
        dmsgs.put("formatted.m", "m");
        dmsgs.put("formatted.t", "t");
        dmsgs.put("formatted.b", "b");
        dmsgs.put("formatted.q", "q");


        messages = new Messages(getDataFolder(), getLogger(), dmsgs);

        getLogger().info("ajLeaderboards v"+getDescription().getVersion()+" by ajgeiss0702 enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ajLeaderboards v"+getDescription().getVersion()+" disabled.");
    }

    public Config getAConfig() {
        return config;
    }

    public Cache getCache() {
        return cache;
    }

    public TopManager getTopManager() {
        return topManager;
    }

    public Messages getMessages() {
        return messages;
    }
}
