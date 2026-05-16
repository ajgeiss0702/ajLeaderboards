package us.ajg0702.leaderboards;

import io.github.slimjar.app.builder.ApplicationBuilder;
import io.github.slimjar.resolver.data.Mirror;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.resolver.mirrors.SimpleMirrorSelector;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.ExtraManager;
import us.ajg0702.leaderboards.commands.main.MainCommand;
import us.ajg0702.leaderboards.displays.armorstands.ArmorStandManager;
import us.ajg0702.leaderboards.displays.heads.HeadManager;
import us.ajg0702.leaderboards.displays.lpcontext.LuckpermsContextLoader;
import us.ajg0702.leaderboards.displays.lpcontext.WithLPCtx;
import us.ajg0702.leaderboards.displays.lpcontext.WithoutLPCtx;
import us.ajg0702.leaderboards.displays.signs.SignManager;
import us.ajg0702.leaderboards.formatting.PlaceholderFormatter;
import us.ajg0702.leaderboards.loaders.MessageLoader;
import us.ajg0702.leaderboards.nms.legacy.HeadUtils;
import us.ajg0702.leaderboards.nms.legacy.ThreadFactoryProxy;
import us.ajg0702.leaderboards.placeholders.PlaceholderExpansion;
import us.ajg0702.leaderboards.utils.*;
import us.ajg0702.utils.common.Config;
import us.ajg0702.utils.common.Messages;
import us.ajg0702.utils.common.UpdateManager;
import us.ajg0702.utils.common.UtilsLogger;
import us.ajg0702.utils.foliacompat.CompatScheduler;
import us.ajg0702.utils.foliacompat.Task;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
    private ResetSaver resetSaver;
    private final Exporter exporter = new Exporter(this);
    private final PlaceholderFormatter placeholderFormatter = new PlaceholderFormatter(this);

    private final Map<String, OfflineUpdater> offlineUpdaters = new ConcurrentHashMap<>();

    private boolean vault;
    private Chat vaultChat;

    private boolean shuttingDown = false;

    private UpdateManager updateManager = null;

    private final CompatScheduler compatScheduler = new CompatScheduler(this);

    private final TrojanScanner trojanScanner = new TrojanScanner(this, getFile());

    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(1, ThreadFactoryProxy.getDefaultThreadFactory("AJLBSCHED"));

    @Override
    public void onLoad() {
        // We scan for this l/M/x trojan on load because it can cause random bugs (especially with dependency loading)
        // Also because it's bad for ppl to have a trojan on their server.
        trojanScanner.scan();

        try {
            Path downloadPath = Paths.get(getDataFolder().getPath() + File.separator + "libs");
            ApplicationBuilder.appending("ajLeaderboards")
                    .logger(new SlimJarLogger(this))
                    .downloadDirectoryPath(downloadPath)
                    .internalRepositories(Collections.singleton(new Repository(new URL("https://repo-mirror.ajg0702.us/mirror/"))))
                    .mirrorSelector((a, b) -> Collections.singleton(new Repository(new URL("https://repo-mirror.ajg0702.us/mirror/"))))
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

        BukkitSender.setAdventure(getAdventure());



        try {
            config = new Config(getDataFolder(), getLogger());
        } catch (ConfigurateException e) {
            getLogger().log(Level.WARNING, "An error occurred while loading your config:", e);
        }

        if (config == null) {
            getLogger().severe("Failed to load config! Please fix the error above and then restart the server.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Debug.setLogger(getLogger());
        Debug.setDebug(config.getBoolean("debug"));
        Debug.setParticles(config.getBoolean("particles"));

        setWeeklyResetDay();

        messages = MessageLoader.loadMessages(this);

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

        getScheduler().runTaskAsynchronously(() -> {
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
        try {
            headUtils = new HeadUtils(getLogger(), new Debug(), compatScheduler);
        } catch (NoClassDefFoundError e) {
            getLogger().warning("Detected NoClassDefFoundError (will be shown below)! Try deleting the 'ajLeaderboards/libs' folder, then check the logs during restart for any errors/warnings (even if it works that time)");
            throw e;
        }
        armorStandManager = new ArmorStandManager(this);

        resetSaver = new ResetSaver(this);

        cache = new Cache(this);

        List<String> initialBoards = cache.getBoards();

        getLogger().info("Loaded "+initialBoards.size()+" boards");

        extraManager = new ExtraManager(this);


        topManager = new TopManager(this, initialBoards);

        reloadInterval();

        scheduleOfflineUpdates();

        getScheduler().runTaskTimerAsynchronously(this::scheduleResets, 0, 15 * 60 * 20);
        getScheduler().runTaskTimerAsynchronously(
                () -> offlineUpdaters.forEach((b, u) -> u.progressLog()),
                5 * 20,
                30 * 20
        );

        Metrics metrics = new Metrics(this, 9338);
        metrics.addCustomChart(new Metrics.SimplePie("storage_method", () -> getCache().getMethod().getName()));
        metrics.addCustomChart(new Metrics.SingleLineChart("boards", () -> getTopManager().getBoards().size()));

        PlaceholderExpansion placeholders = new PlaceholderExpansion(this);
        if(placeholders.register()) {
            getLogger().info("PAPI placeholders successfully registered!");
        } else {
            getLogger().warning("Failed to register ajLeaderboard PAPI placeholders! Leaderboard display placeholders will not work!");
        }

        contextLoader = Bukkit.getPluginManager().isPluginEnabled("LuckPerms") ? new WithLPCtx(this) : new WithoutLPCtx(this);
        contextLoader.checkReload();

        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);

        if(config.getBoolean("enable-updater")) {
            updateManager = new UpdateManager(utilsLogger, getDescription().getVersion(), "ajLeaderboards", "ajLeaderboards", null, getDataFolder().getParentFile(), "ajLeaderboards update");
        }

        getLogger().info("ajLeaderboards v"+getDescription().getVersion()+" by ajgeiss0702 enabled!");


        // We want to scan for the trojan again well after the server starts, because it infects jars after the server starts,
        //  and older versions of the trojan break the ajLeaderboards jar (meaning this might be our only chance).
        // I'm hoping this will reduce the number of people coming to support about this.

        // Run the trojan scanner 2 minutes after loading in case we get infected not too long after loading
        getScheduler().runTaskLaterAsynchronously(trojanScanner::scan, 2 * 60 * 20);

        // Run the trojan scanner on an interval in case we get infected way later
        getScheduler().runTaskTimerAsynchronously(trojanScanner::scan, 60 * 60 * 20, 6 * 60 * 60 * 120);
    }

    private Iterable<String> getSignPath(int i) {
        return getSignPath(i+"");
    }
    private Iterable<String> getSignPath(String end) {
        return Arrays.asList("signs", "top", end);
    }

    @Override
    public void onDisable() {
        boolean fastShutdown = getAConfig().getBoolean("fast-shutdown");
        shuttingDown = true;
        if(getContextLoader() != null) getContextLoader().checkReload(false);
        getScheduler().cancelTasks();
        scheduledExecutorService.shutdown();
        if(getTopManager() != null) getTopManager().shutdown(fastShutdown);
        try {
            scheduledExecutorService.awaitTermination(fastShutdown ? 1 : 10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        if(getCache() != null) {
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
        }

        if(!fastShutdown) {
            getLogger().info("Killing remaining workers");
            killWorkers(1000);
            Debug.info("1st kill pass done, retrying for remaining");
            killWorkers(5000);
            getLogger().info("Remaining workers killed");
        } else {
            killWorkers(100);
            getLogger().warning("Fast shutdown is enabled! If you see warnings/errors to nag me about shutting down async tasks, you should be able to ignore them. Disable fast-shutdown if you don't want to see those warnings/errors or this message.");
        }

        getLogger().info("ajLeaderboards v"+getDescription().getVersion()+" disabled.");

        getScheduler().getActiveWorkers().forEach(bukkitWorker -> {
            Debug.info("Active worker: "+bukkitWorker.getOwner().getDescription().getName()+" "+bukkitWorker.getTaskId());
            for (StackTraceElement stackTraceElement : bukkitWorker.getThread().getStackTrace()) {
                Debug.info(" - "+stackTraceElement);
            }
        });
    }

    private void killWorkers(int waitForDeath) {
        List<BukkitWorker> workers = new ArrayList<>(getScheduler().getActiveWorkers());
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

    public Exporter getExporter() {
        return exporter;
    }

    public PlaceholderFormatter getPlaceholderFormatter() {
        return placeholderFormatter;
    }

    public Map<String, OfflineUpdater> getOfflineUpdaters() {
        return offlineUpdaters;
    }

    public ResetSaver getResetSaver() {
        return resetSaver;
    }

    public CompatScheduler getCompatScheduler() {
        return compatScheduler;
    }

    public CompatScheduler getScheduler() {
        return getCompatScheduler();
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    Task updateTask;
    public void reloadInterval() {
        if(updateTask != null) {
            try {
                updateTask.cancel();
            } catch(IllegalArgumentException ignored) {}
            updateTask = null;
        }
        updateTask = getScheduler().runTaskTimerAsynchronously(() -> {
            if(!config.getBoolean("update-stats")) return;
            if(getTopManager().getFetchingAverage() > 100) {
                getLogger().warning("Database is overloaded! Skipping update of players.");
                return;
            }
            for(Player p : Bukkit.getOnlinePlayers()) {
                if(isShuttingDown()) return;
                getTopManager().submit(() -> getCache().updatePlayerStats(p));
            }
        }, 10*20, config.getInt("stat-refresh"));
    }

    public void scheduleOfflineUpdates() {
        int intervalHours = config.getInt("offline-update-interval-hours");
        boolean runOnStartup = config.getBoolean("offline-update-run-on-startup");
        List<String> boards = config.getStringList("offline-update-boards");

        // If no boards specified, feature is disabled
        if(boards.isEmpty()) {
            return;
        }

        // Validate boards
        List<String> validBoards = new ArrayList<>();
        for(String board : boards) {
            if(getTopManager().boardExists(board)) {
                validBoards.add(board);
            } else {
                getLogger().warning("Invalid board '" + board + "' in offline-update-boards config. Skipping.");
            }
        }

        if(validBoards.isEmpty()) {
            getLogger().warning("No valid boards configured for offline updates. Disabling feature.");
            return;
        }

        long intervalTicks = intervalHours * 72000L;

        // For startup run: initial delay = 30 seconds, then run at interval
        // For interval-only: initial delay = interval
        long initialDelay = runOnStartup ? 30 * 20L : intervalTicks;

        getScheduler().runTaskTimerAsynchronously(() -> {
            if(isShuttingDown()) return;

            for(String board : validBoards) {
                if(isShuttingDown()) return;

                if(offlineUpdaters.containsKey(board)) {
                    getLogger().warning("[OfflineUpdater] " + board + ": Already running, skipping this cycle. Consider increasing offline-update-interval-hours.");
                    continue;
                }

                OfflinePlayer[] players = Bukkit.getOfflinePlayers();
                offlineUpdaters.put(board, new OfflineUpdater(this, board, players, null));
            }
        }, initialDelay, intervalTicks);

        getLogger().info("Offline player updates scheduled for boards: " + validBoards + " (every " + intervalHours + " hours)");
    }

    final HashMap<TimedType, Task> resetTasks = new HashMap<>();
    public void scheduleResets() {
        resetTasks.values().forEach(Task::cancel);
        resetTasks.clear();

        for(TimedType type : TimedType.values()) {
            try {
                scheduleReset(type);
            } catch (ExecutionException | InterruptedException e) {
                if(isShuttingDown()) return;
                getLogger().log(Level.WARNING, "Scheduling reset interrupted:", e);
            }
        }
    }

    public void scheduleReset(TimedType type) throws ExecutionException, InterruptedException {
        if(type.equals(TimedType.ALLTIME)) return;

        long now = Instant.now().getEpochSecond();

        long nextReset = type.getNextResetEpochSeconds();

        List<String> resetNow = new ArrayList<>();

        for (String board : getTopManager().getBoards()) {
            long lastReset = topManager.getLastReset(board, type);
            long estLastReset = type.getEstimatedLastResetEpochSeconds();

            if(lastReset < estLastReset) {
                Debug.info("lastRest for "+type+" "+board+" is before estimatedLastReset! "+lastReset+" < "+estLastReset);
                resetNow.add(board);
            }
        }

        if(resetNow.size() > 0) {
            Debug.info("Resetting " + type + " due to lastReset being before estimatedLastReset");
            for (String board : resetNow) {
                topManager.submit(() -> {
                    try {
                        cache.reset(board, type);
                    } catch (ExecutionException | InterruptedException e) {
                        if(isShuttingDown()) return;
                        getLogger().log(Level.WARNING, "Unable to reset "+type+" for "+board+": (interupted/exception)", e);
                    }
                });
            }
        }

        if(isShuttingDown()) return;

        long secsTilNextReset = nextReset - now;
        Debug.info("Initial secsTilNextReset for "+type.lowerName()+": "+secsTilNextReset);
        if(secsTilNextReset < 0) {
            secsTilNextReset = 0;
        }


        Debug.info(TimeUtils.formatTimeSeconds(secsTilNextReset)+" until the reset for "+type.lowerName()+" (next formatted: "+type.getNextReset().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)+" next: "+nextReset+")");

        if(isShuttingDown()) return;
        // don't bother scheduling the reset if it's more than 16 minutes away (since resets are scheduled every 15 minutes)
        if(secsTilNextReset > 16 * 60) return;
        Task task = getScheduler().runTaskLaterAsynchronously(
                () -> {
                    for (String board : getTopManager().getBoards()) {
                        topManager.submit(() -> {
                            try {
                                cache.reset(board, type);
                            } catch (ExecutionException | InterruptedException e) {
                                if(isShuttingDown()) return;
                                getLogger().log(Level.WARNING, "Unable to reset "+type+" for "+board+": (interupted/exception)", e);
                            }
                        });
                    }
                },
                secsTilNextReset*20L
        );
        resetTasks.put(type, task);
    }

    public boolean validatePlaceholder(String placeholder, CommandSender sayOutput) {
        if(Bukkit.getOnlinePlayers().size() == 0) {
            getLogger().warning("Unable to validate placeholder because no players are online. Skipping validation.");
            return true;
        }
        Player vp = Bukkit.getOnlinePlayers().iterator().next();
        String out = PlaceholderAPI.setPlaceholders(vp, "%"+ Cache.alternatePlaceholders(placeholder)+"%").replaceAll(",", "");
        try {
            getPlaceholderFormatter().toDouble(out, placeholder);
        } catch(NumberFormatException e) {
            Debug.info(e.getMessage());
            if(sayOutput != null) {
                sayOutput.sendMessage(message("&7Returned: "+out.replaceAll("§", "&")));
            }
            return false;
        }
        return true;
    }

    private static MiniMessage miniMessage;
    public static MiniMessage getMiniMessage() {
        if(miniMessage == null) {
            miniMessage = MiniMessage.miniMessage();
        }
        return miniMessage;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    private static BukkitAudiences adventure;

    public BukkitAudiences getAdventure() {
        if(adventure == null) {
            adventure = BukkitAudiences.create(this);
        }
        return adventure;
    }

    public void setWeeklyResetDay() {
        String rawDay = config.getString("reset-weekly-on");
        DayOfWeek day;
        try {
            day = DayOfWeek.valueOf(rawDay.toUpperCase(Locale.ROOT));
        } catch(IllegalArgumentException e) {
            getLogger().warning("Invalid day '"+rawDay+"' for reset-weekly-on in the config! Defaulting to sunday.");
            day = DayOfWeek.SUNDAY;
        }
        TimedType.setWeeklyResetDay(day);
    }


    public static Component message(String miniMessage) {
        return getMiniMessage().deserialize(Messages.color(miniMessage));
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    private long lastTimeAlert = 0;
    private boolean doublePrevention = false; // without this, in testing, the message appeared twice about 90% of the time
    public void timePlaceholderUsed() {
        if(doublePrevention) return;
        doublePrevention = true;
        long timeAlertCooldown = 30 * TimeUtils.MINUTE;

        if(lastTimeAlert == 0) { // delay first alert by 30 seconds
            lastTimeAlert = System.currentTimeMillis() - (timeAlertCooldown - (30 * TimeUtils.SECOND));
        }
        if(System.currentTimeMillis() - lastTimeAlert > timeAlertCooldown) {
            lastTimeAlert = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if(!player.hasPermission("ajleaderboards.use")) continue;
                getAdventure().player(player).sendMessage(message(
                        "&6[&eajLeaderboards&6] &cYou are using a deprecated placeholder! " +
                                "&7The time placeholder is no longer necessary, and will be removed in the future.\n" +
                                "&fTo replace it&7, replace all time placeholders with value placeholders. " +
                                "They will automatically format the time.\n" +
                                "\n" +
                                "&eFor more information, &6" +
                                "<click:open_url:'https://wiki.ajg0702.us/ajleaderboards/time-deprecation'><underlined>click here</click>\n"
                ));
            }
        }
        doublePrevention = false;
    }

    public Future<Material> safeGetBlockType(Location location) {
        CompletableFuture<Material> future = new CompletableFuture<>();
        Runnable runnable = () -> {
            future.complete(location.getBlock().getType());
        };
        if(CompatScheduler.isFolia()) {
            getScheduler().runSync(location, runnable);
        } else {
            runnable.run();
        }
        return future;
    }

    private UtilsLogger utilsLogger = new UtilsLogger() {
        @Override
        public void warn(String s) {
            getLogger().warning(s);
        }

        @Override
        public void warning(String s) {
            getLogger().warning(s);
        }

        @Override
        public void info(String s) {
            getLogger().info(s);
        }

        @Override
        public void error(String s) {
            getLogger().severe(s);
        }

        @Override
        public void severe(String s) {
            getLogger().severe(s);
        }
    };
}
