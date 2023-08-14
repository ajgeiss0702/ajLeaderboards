package us.ajg0702.leaderboards;

import io.github.slimjar.app.builder.ApplicationBuilder;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.resolver.mirrors.SimpleMirrorSelector;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.chat.Chat;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
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
import us.ajg0702.leaderboards.boards.keys.BoardType;
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
import us.ajg0702.leaderboards.placeholders.PlaceholderExpansion;
import us.ajg0702.leaderboards.utils.Exporter;
import us.ajg0702.leaderboards.utils.OfflineUpdater;
import us.ajg0702.leaderboards.utils.ResetSaver;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
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
    private ResetSaver resetSaver;
    private final Exporter exporter = new Exporter(this);
    private final PlaceholderFormatter placeholderFormatter = new PlaceholderFormatter(this);

    private final Map<String, OfflineUpdater> offlineUpdaters = new ConcurrentHashMap<>();

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

        BukkitSender.setAdventure(getAdventure());



        try {
            config = new Config(getDataFolder(), getLogger());
        } catch (ConfigurateException e) {
            getLogger().log(Level.WARNING, "An error occurred while loading your config:", e);
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

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
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

        resetSaver = new ResetSaver(this);

        cache = new Cache(this);

        List<String> initialBoards = cache.getBoards();

        getLogger().info("Loaded "+initialBoards.size()+" boards");

        extraManager = new ExtraManager(this);


        topManager = new TopManager(this, initialBoards);

        reloadInterval();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::scheduleResets, 0, 15 * 60 * 20);
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                () -> offlineUpdaters.forEach((b, u) -> u.progressLog()),
                5 * 20,
                30 * 20
        );

        Metrics metrics = new Metrics(this, 9338);
        metrics.addCustomChart(new Metrics.SimplePie("storage_method", () -> getCache().getMethod().getName()));

        PlaceholderExpansion placeholders = new PlaceholderExpansion(this);
        if(placeholders.register()) {
            getLogger().info("PAPI placeholders successfully registered!");
        } else {
            getLogger().warning("Failed to register ajlb PAPI placeholders!");
        }

        contextLoader = Bukkit.getPluginManager().isPluginEnabled("LuckPerms") ? new WithLPCtx(this) : new WithoutLPCtx(this);
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
        if(getContextLoader() != null) getContextLoader().checkReload(false);
        Bukkit.getScheduler().cancelTasks(this);
        if(getTopManager() != null) getTopManager().shutdown();

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

    final HashMap<TimedType, Integer> resetIds = new HashMap<>();
    public void scheduleResets() {
        resetIds.values().forEach(Bukkit.getScheduler()::cancelTask);
        resetIds.clear();

        for(TimedType type : TimedType.values()) {
            try {
                scheduleReset(type);
            } catch (ExecutionException | InterruptedException e) {
                if(isShuttingDown()) return;
                getLogger().log(Level.WARNING, "Scheduling reset interupted:", e);
            }
        }
    }

    public void scheduleReset(TimedType type) throws ExecutionException, InterruptedException {
        if(type.equals(TimedType.ALLTIME)) return;

        long now = Instant.now().getEpochSecond();

        long nextReset = type.getNextReset().toEpochSecond(TimeUtils.getDefaultZoneOffset());

        List<String> resetNow = new ArrayList<>();

        for (String board : getTopManager().getBoards()) {
            long lastReset = topManager.getLastReset(board, type);
            LocalDateTime lastResetDate = LocalDateTime.ofEpochSecond(lastReset, 0, ZoneOffset.UTC);
            long estLastReset = type.getEstimatedLastReset().toEpochSecond(TimeUtils.getDefaultZoneOffset());
            long lastResetConverted = lastResetDate.toEpochSecond(TimeUtils.getDefaultZoneOffset());

            if(lastResetConverted < estLastReset) {
                Debug.info("lastRest for "+type+" "+board+" is before estimatedLastReset! "+lastReset+" < "+estLastReset);
                resetNow.add(board);
            }
        }

        if(resetNow.size() > 0) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    for (String board : resetNow) {
                        cache.reset(board, type);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    if(isShuttingDown()) return;
                    getLogger().log(Level.WARNING, "Unable to reset "+type+": (interupted/exception)", e);
                }
            });
        }

        if(isShuttingDown()) return;

        long secsTilNextReset = nextReset - now;
        Debug.info("Initial secsTilNextReset for "+type.lowerName()+": "+secsTilNextReset);
        if(secsTilNextReset < 0) {
            secsTilNextReset = 0;
        }


        Debug.info(TimeUtils.formatTimeSeconds(secsTilNextReset)+" until the reset for "+type.lowerName()+" (next formatted: "+type.getNextReset().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)+" next: "+nextReset+")");

        if(isShuttingDown()) return;
        int taskId = Bukkit.getScheduler().runTaskLaterAsynchronously(
                this,
                () -> {
                    try {
                        for (String board : getTopManager().getBoards()) {
                            cache.reset(board, type);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        if(isShuttingDown()) return;
                        getLogger().log(Level.WARNING, "Unable to reset "+type+": (interupted/exception)", e);
                    }
                },
                secsTilNextReset*20L
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
            getPlaceholderFormatter().toDouble(out, placeholder);
        } catch(NumberFormatException e) {
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
}
