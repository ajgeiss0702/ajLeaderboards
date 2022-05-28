package us.ajg0702.leaderboards;

import io.github.slimjar.app.builder.ApplicationBuilder;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.resolver.mirrors.SimpleMirrorSelector;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.chat.Chat;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitWorker;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.platforms.bukkit.BukkitCommand;
import us.ajg0702.commands.platforms.bukkit.BukkitSender;
import us.ajg0702.leaderboards.boards.BoardType;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.ExtraManager;
import us.ajg0702.leaderboards.commands.main.MainCommand;
import us.ajg0702.leaderboards.displays.armorstands.ArmorStandManager;
import us.ajg0702.leaderboards.displays.heads.HeadManager;
import us.ajg0702.leaderboards.displays.lpcontext.LuckpermsContextLoader;
import us.ajg0702.leaderboards.displays.signs.SignManager;
import us.ajg0702.leaderboards.nms.HeadUtils;
import us.ajg0702.leaderboards.placeholders.PlaceholderExpansion;
import us.ajg0702.leaderboards.utils.SlimJarLogger;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeaderboardPlugin extends JavaPlugin {

    private Config config;
    private Cache cache;
    private ExtraManager extraManager;
    private Messages messages;
    private TopManager topManager;
    private SignManager signManager;
    private HeadManager headManager;
    private HeadUtils headUtils;
    private ArmorStandManager armorStandManager;
    private LuckpermsContextLoader contextLoader;

    private boolean vault;
    private Chat vaultChat;

    private boolean shuttingDown = false;

    @Override
    public void onLoad() {
        try {
            Path downloadPath = Paths.get(getDataFolder().getPath() + File.separator + "libs");
            ApplicationBuilder.appending("ajLeaderboards")
                    .logger(new SlimJarLogger(this))
                    .downloadDirectoryPath(downloadPath)
                    .mirrorSelector((a, b) -> a)
                    .internalRepositories(Collections.singleton(new Repository(new URL(SimpleMirrorSelector.ALT_CENTRAL_URL))))
                    .build();
        } catch (IOException | ReflectiveOperationException | URISyntaxException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {

        if(isShuttingDown()) {
            throw new IllegalStateException("Reload was used! ajLeaderboards does not support this!");
        }
        StatEntry.setPlugin(this);

        BukkitCommand bukkitMainCommand = new BukkitCommand(new MainCommand(this));
        bukkitMainCommand.register(this);

        BukkitSender.setAdventure(this);



        try {
            config = new Config(getDataFolder(), getLogger());
        } catch (ConfigurateException e) {
            getLogger().log(Level.WARNING, "An error occurred while loading your config:", e);
        }

        Debug.setLogger(getLogger());
        Debug.setDebug(config.getBoolean("debug"));
        Debug.setParticles(config.getBoolean("particles"));

        Map<String, Object> dmsgs = new LinkedHashMap<>();

        dmsgs.put("signs.top.default", Arrays.asList(
                "&7&m       &r #{POSITION} &7&m       ",
                "&6{NAME}",
                "&e{VALUE} {VALUENAME}",
                "&7&m                   "
        ));

        dmsgs.put("formatted.k", "k");
        dmsgs.put("formatted.m", "m");
        dmsgs.put("formatted.t", "t");
        dmsgs.put("formatted.b", "b");
        dmsgs.put("formatted.q", "q");

        dmsgs.put("time.w", "w ");
        dmsgs.put("time.d", "d ");
        dmsgs.put("time.h", "h ");
        dmsgs.put("time.m", "m ");
        dmsgs.put("time.s", "s");

        dmsgs.put("noperm", "You don't have permission to do this!");

        dmsgs.put("commands.reload.success", "&aConfigs reloaded!");
        dmsgs.put("commands.reload.fail", "&cAn error occurred while reloading one of your configs. Check the console for more info.");

        dmsgs.put("commands.export.fileexists", "&cThe file &f{FILE}&c already exists!");
        dmsgs.put("commands.export.starting", "&7Fetching all players from the datbase. This might take a bit.");
        dmsgs.put("commands.export.fail", "&cAn error occurred while exporting. Check the console for more info.");
        dmsgs.put("commands.export.progress", "&eProgress: &f{DONE}&7/&f{TOTAL}&7 boards fetched");
        dmsgs.put("commands.export.success", "&aThe cache has been exported to the file &f{FILE}&a!");

        dmsgs.put("commands.import.nofile", "&cThe file &f{FILE}&c doesnt exist!");
        dmsgs.put("commands.import.starting", "&7Loading all cached stats from &f{FILE}");
        dmsgs.put("commands.import.fail", "&cAn error occurred while importing. Check the console for more info.");
        dmsgs.put("commands.import.insertprogress", "&eProgress: &f{DONE}&7/&f{TOTAL}&7 boards imported");
        dmsgs.put("commands.import.success", "&aThe cache has been imported from the file &f{FILE}&a!");



        messages = new Messages(getDataFolder(), getLogger(), dmsgs);

        CommentedConfigurationNode msgs = messages.getRootNode();

        if(msgs.hasChild(getSignPath("1"))) {
            List<String> linesList = new ArrayList<>();

            for (int i = 1; i <= 4; i++) {
                linesList.add(msgs.node(getSignPath(i)).getString());
            }

            try {
                msgs.node(getSignPath("default")).setList(String.class, linesList);
                for(int i = 1; i <= 4; i++) {
                    msgs.node(getSignPath(i)).set(null);
                }
                messages.save();
            } catch (SerializationException e) {
                getLogger().log(Level.SEVERE, "Unable to move sign messages: ", e);
            }

        }


        TimeUtils.setStrings(messages);

        Bukkit.getScheduler().runTask(this, () -> {
            if(Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
                if(rsp == null) {
                    vault = false;
                    getLogger().warning("Vault prefix hook failed! Make sure you have a plugin that implements chat (e.g. Luckperms)");
                } else {
                    vaultChat = rsp.getProvider();
                    vault = true;
                }
            }
        });

        signManager = new SignManager(this);
        headManager = new HeadManager(this);
        headUtils = new HeadUtils(getLogger());
        armorStandManager = new ArmorStandManager(this);

        cache = new Cache(this);
        getLogger().info("Loaded "+cache.getBoards().size()+" boards");

        extraManager = new ExtraManager(this);


        topManager = new TopManager(this);

        reloadInterval();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::scheduleResets, 0, 15 * 60 * 20);

        Metrics metrics = new Metrics(this, 9338);
        metrics.addCustomChart(new Metrics.SimplePie("storage_method", () -> getCache().getMethod().getName()));

        PlaceholderExpansion placeholders = new PlaceholderExpansion(this);
        if(placeholders.register()) {
            getLogger().info("PAPI placeholders successfully registered!");
        } else {
            getLogger().warning("Failed to register ajlb PAPI placeholders!");
        }

        contextLoader = new LuckpermsContextLoader(this);
        contextLoader.checkReload();

        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);

        getLogger().info("ajLeaderboards v"+getDescription().getVersion()+" by ajgeiss0702 enabled!");
    }

    private Iterable<String> getSignPath(int i) {
        return getSignPath(i+"");
    }
    private Iterable<String> getSignPath(String end) {
        return Arrays.asList("signs", "top", end);
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        Bukkit.getScheduler().cancelTasks(this);
        getTopManager().shutdown();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            getLogger().info("Shutting down cache method..");
            getCache().getMethod().shutdown();
            getLogger().info("Cache method shut down");
        });
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                getLogger().warning("Cache took too long to shut down. Skipping it.");
            }
        }catch(InterruptedException ignored){}
        getLogger().info("Killing remaining workers");
        killWorkers(1000);
        Debug.info("1st kill pass done, retrying for remaining");
        killWorkers(5000);
        getLogger().info("Remaining workers killed");
        getLogger().info("ajLeaderboards v"+getDescription().getVersion()+" disabled.");
        Bukkit.getScheduler().getActiveWorkers().forEach(bukkitWorker -> {
            Debug.info("Active worker: "+bukkitWorker.getOwner().getDescription().getName()+" "+bukkitWorker.getTaskId());
            for (StackTraceElement stackTraceElement : bukkitWorker.getThread().getStackTrace()) {
                Debug.info(" - "+stackTraceElement);
            }
        });
    }

    private void killWorkers(int waitForDeath) {
        List<BukkitWorker> workers = new ArrayList<>(Bukkit.getScheduler().getActiveWorkers());
        List<Integer> killedWorkers = new ArrayList<>();
        workers.forEach(bukkitWorker -> {
            if(!bukkitWorker.getOwner().equals(this)) return;
            int id = bukkitWorker.getTaskId();
            if(killedWorkers.contains(id)) return;
            Debug.info("Got worker "+id);
            try {
                bukkitWorker.getThread().interrupt();
                Debug.info("Interupted");
                bukkitWorker.getThread().join(waitForDeath);
                Debug.info("Death");
            } catch(SecurityException e) {
                Debug.info("denied: "+e.getMessage());
            } catch (InterruptedException ignored) {
                Debug.info("threw interupted exception on "+id);
            }
            killedWorkers.add(id);
        });
    }

    public Config getAConfig() {
        return config;
    }

    public Cache getCache() {
        return cache;
    }

    public ExtraManager getExtraManager() {
        return extraManager;
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

    public LuckpermsContextLoader getContextLoader() {
        return contextLoader;
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
            if(!config.getBoolean("update-stats")) return;
            if(getTopManager().getFetchingAverage() > 100) {
                getLogger().warning("Database is overloaded! Skipping update of players.");
                return;
            }
            for(Player p : Bukkit.getOnlinePlayers()) {
                if(isShuttingDown()) return;
                getTopManager().submit(() -> getCache().updatePlayerStats(p));
            }
        }, 10*20, config.getInt("stat-refresh")).getTaskId();
        Debug.info("Update task id is "+updateTaskId);
    }

    final HashMap<BoardType, Integer> resetIds = new HashMap<>();
    public void scheduleResets() {
        resetIds.values().forEach(Bukkit.getScheduler()::cancelTask);
        resetIds.clear();

        for(String board : cache.getBoards()) {
            for(TimedType type : TimedType.values()) {
                try {
                    scheduleReset(board, type);
                } catch (ExecutionException | InterruptedException e) {
                    if(isShuttingDown()) return;
                    getLogger().log(Level.WARNING, "Scheduling reset interupted:", e);
                }
            }
        }
    }

    public void scheduleReset(String board, TimedType type) throws ExecutionException, InterruptedException {
        if(type.equals(TimedType.ALLTIME)) return;

        long now = Instant.now().getEpochSecond();


        long lastReset = topManager.getLastReset(board, type).get();
        long nextReset = type.getNextReset().toEpochSecond(TimeUtils.getDefaultZoneOffset());

        if(isShuttingDown()) return;

        long secsTilNextReset = nextReset - now;
        Debug.info("Initial secsTilNextReset for "+type.lowerName()+" "+board+": "+secsTilNextReset);
        if(secsTilNextReset < 0) {
            secsTilNextReset = 0;
        }


        LocalDateTime lastResetDate = LocalDateTime.ofEpochSecond(lastReset, 0, ZoneOffset.UTC);
        long estLastReset = type.getEstimatedLastReset().toEpochSecond(TimeUtils.getDefaultZoneOffset());
        long lastResetConverted = lastResetDate.toEpochSecond(TimeUtils.getDefaultZoneOffset());
        if(lastResetConverted < estLastReset) {
            Debug.info("lastRest for "+type+" "+board+" is before estimatedLastReset! "+lastReset+" < "+estLastReset);
            secsTilNextReset = 0;
        }

        Debug.info(TimeUtils.formatTimeSeconds(secsTilNextReset)+" until the reset for "+board+" "+type.lowerName()+" (next: "+type.getNextReset().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)+" last: "+ lastResetDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME) +" last: "+lastReset+" next: "+nextReset+")");

        if(isShuttingDown()) return;
        int taskId = Bukkit.getScheduler().runTaskLaterAsynchronously(
                this,
                () -> {
                    try {
                        cache.reset(board, type);
                    } catch (ExecutionException | InterruptedException e) {
                        if(isShuttingDown()) return;
                        getLogger().log(Level.WARNING, "Unable to reset "+type+" for "+board+": (interupted/exception)", e);
                    }
                },
                secsTilNextReset*20L
        ).getTaskId();
        resetIds.put(new BoardType(board, type), taskId);
    }

    public boolean validatePlaceholder(String placeholder, CommandSender sayOutput) {
        if(Bukkit.getOnlinePlayers().size() == 0) {
            getLogger().warning("Unable to validate placeholder because no players are online. Skipping validation.");
            return true;
        }
        Player vp = Bukkit.getOnlinePlayers().iterator().next();
        String out = PlaceholderAPI.setPlaceholders(vp, "%"+ Cache.alternatePlaceholders(placeholder)+"%").replaceAll(",", "");
        try {
            Double.valueOf(convertPlaceholderOutput(out));
        } catch(NumberFormatException e) {
            if(sayOutput != null) {
                sayOutput.sendMessage(message("&7Returned: "+out.replaceAll("§", "&")));
            }
            return false;
        }
        return true;
    }

    private final static Pattern weekPattern = Pattern.compile("([1-9][0-9]*)w");
    private final static Pattern dayPattern = Pattern.compile("([1-9][0-9]*)d");
    private final static Pattern hourPattern = Pattern.compile("([1-9][0-9]*)h");
    private final static Pattern minutePattern = Pattern.compile("([1-9][0-9]*)m");
    private final static Pattern secondPattern = Pattern.compile("([1-9][0-9]*)s");
    public static String convertPlaceholderOutput(String output) {
        int seconds = -1;

        seconds = getSeconds(output, 60*60*24*7, seconds, weekPattern);
        seconds = getSeconds(output, 60*60*24, seconds, dayPattern);
        seconds = getSeconds(output, 60*60, seconds, hourPattern);
        seconds = getSeconds(output, 60, seconds, minutePattern);
        seconds = getSeconds(output, 1, seconds, secondPattern);

        if(seconds != -1) return seconds+"";
        return output;
    }

    private static int getSeconds(String output, int multiplier, int seconds, Pattern pattern) {
        Matcher matcher = pattern.matcher(output);
        if(matcher.find()) {
            if(seconds == -1) seconds = 0;
            seconds += Integer.parseInt(matcher.group(1))*multiplier;
        }
        return seconds;
    }

    private static MiniMessage miniMessage;
    public static MiniMessage getMiniMessage() {
        if(miniMessage == null) {
            miniMessage = MiniMessage.miniMessage();
        }
        return miniMessage;
    }


    public static Component message(String miniMessage) {
        return getMiniMessage().deserialize(Messages.color(miniMessage));
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }
}
