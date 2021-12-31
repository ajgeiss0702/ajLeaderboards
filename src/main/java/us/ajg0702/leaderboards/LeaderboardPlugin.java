package us.ajg0702.leaderboards;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.platforms.bukkit.BukkitCommand;
import us.ajg0702.commands.platforms.bukkit.BukkitSender;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.commands.main.MainCommand;
import us.ajg0702.leaderboards.displays.armorstands.ArmorStandManager;
import us.ajg0702.leaderboards.displays.heads.HeadManager;
import us.ajg0702.leaderboards.displays.heads.HeadUtils;
import us.ajg0702.leaderboards.displays.signs.SignManager;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class LeaderboardPlugin extends JavaPlugin {

    private Config config;
    private Cache cache;
    private Messages messages;
    private TopManager topManager;
    private SignManager signManager;
    private HeadManager headManager;
    private HeadUtils headUtils;
    private ArmorStandManager armorStandManager;

    private boolean vault;
    private Chat vaultChat;

    @Override
    public void onEnable() {

        BukkitCommand bukkitMainCommand = new BukkitCommand(new MainCommand(this));

        PluginCommand mainCommand = getCommand("ajleaderboards");
        assert mainCommand != null;
        mainCommand.setExecutor(bukkitMainCommand);
        mainCommand.setTabCompleter(bukkitMainCommand);

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

        dmsgs.put("noperm", "You don't have permission to do this!");

        dmsgs.put("commands.reload.success", "&aConfigs reloaded!");
        dmsgs.put("commands.reload.fail", "&cAn error occurred while reloading one of your configs. Check the console for more info.");

        messages = new Messages(getDataFolder(), getLogger(), dmsgs);

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

        reloadInterval();

        signManager = new SignManager(this);
        headManager = new HeadManager(this);
        headUtils = new HeadUtils();
        armorStandManager = new ArmorStandManager(this);

        scheduleResets();



        getLogger().info("ajLeaderboards v"+getDescription().getVersion()+" by ajgeiss0702 enabled!");
    }

    @Override
    public void onDisable() {
        getCache().getMethod().shutdown();
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

    public boolean hasVault() {
        return vault;
    }

    public SignManager getSignManager() {
        return signManager;
    }

    public HeadUtils getHeadUtils() {
        return headUtils;
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public HeadManager getHeadManager() {
        return headManager;
    }

    public Chat getVaultChat() {
        return vaultChat;
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
                getCache().updatePlayerStats(p);
            }
        }, 10*20, config.getInt("stat-refresh")).getTaskId();
    }

    HashMap<TimedType, Integer> resetIds = new HashMap<>();
    public void scheduleResets() {
        resetIds.values().forEach(Bukkit.getScheduler()::cancelTask);
        resetIds.clear();

        for(String board : cache.getBoards()) {
            for(TimedType type : TimedType.values()) {
                scheduleReset(board, type);
            }
        }
    }

    public void scheduleReset(String board, TimedType type) {
        if(type.equals(TimedType.ALLTIME)) return;
        if(type.getResetMs() < 0) return;


        long lastReset = cache.getLastReset(board, type);
        long nextReset = lastReset + type.getResetMs();
        int timeTilNextReset = (int) (nextReset - System.currentTimeMillis());
        if(timeTilNextReset < 0) {
            timeTilNextReset = 0;
        }

        int taskId = Bukkit.getScheduler().runTaskLaterAsynchronously(
                this,
                () -> cache.reset(board, type),
                (long) ((timeTilNextReset/1000D)*20)
        ).getTaskId();
        resetIds.put(type, taskId);
    }

    public boolean validatePlaceholder(String placeholder, CommandSender sayOutput) {
        if(Bukkit.getOnlinePlayers().size() == 0) {
            getLogger().warning("Unable to validate placeholder because no players are online. Skipping validation.");
            return true;
        }
        Player vp = Bukkit.getOnlinePlayers().iterator().next();
        String out = PlaceholderAPI.setPlaceholders(vp, "%"+ Cache.alternatePlaceholders(placeholder)+"%").replaceAll(",", "");
        try {
            Double.valueOf(out);
        } catch(NumberFormatException e) {
            if(sayOutput != null) {
                sayOutput.sendMessage(message("&7Returned: "+out));
            }
            return false;
        }
        return true;
    }


    public static Component message(String miniMessage) {
        return MiniMessage.get().parse(ChatColor.translateAlternateColorCodes('&', miniMessage));
    }

}
