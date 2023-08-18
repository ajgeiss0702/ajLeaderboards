package us.ajg0702.leaderboards.displays.signs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.utils.common.Messages;
import us.ajg0702.utils.spigot.VersionSupport;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Matcher;

public class SignManager {
    private final LeaderboardPlugin plugin;
    private YamlConfiguration cfg;
    private File cfgFile;

    private static final LegacyComponentSerializer LEGACY_SIGN_SERIALIZER = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final List<BoardSign> signs = new CopyOnWriteArrayList<>();

    public SignManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTask(plugin, this::reload);


    }

    public List<BoardSign> getSigns() {
        return signs;
    }

    int updateIntervalId = -1;

    public void reload() {
        cfgFile = new File(plugin.getDataFolder(), "displays.yml");
        cfg = YamlConfiguration.loadConfiguration(cfgFile);
        String headerText = "This file is for storing sign location, npcs, and other things in the plugin that might display data";
        if(VersionSupport.getMinorVersion() > 18) {
            cfg.options().setHeader(Collections.singletonList(headerText));
        } else {
            //noinspection deprecation
            cfg.options().header(headerText);
        }

        signs.clear();
        if(cfg.contains("signs")) {
            List<String> rawsigns = cfg.getStringList("signs");
            for(String s : rawsigns) {
                try {
                    signs.add(BoardSign.deserialize(s));
                } catch(Exception e) {
                    plugin.getLogger().log(Level.WARNING, "An error occurred while loading a sign:", e);
                }
            }
        }
        updateNameCache();

        if(updateIntervalId != -1) {
            try {
                Bukkit.getScheduler().cancelTask(updateIntervalId);
                updateIntervalId = -1;
            } catch(IllegalStateException e) {
                updateIntervalId = -1;
            }
        }
        updateIntervalId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateSigns, 10*20, plugin.getAConfig().getInt("sign-update")).getTaskId();
    }

    /**
     * Remove a sign
     * Clears the text on the sign by default
     * (schedules a task to remove the text on the next tick)
     * @param l The location of the sign to remove
     * @return If the sign was removed or not. If false, the target location was not an ajLeaderboards sign
     */
    public boolean removeSign(Location l) {
        return removeSign(l, true);
    }

    /**
     * Remove a sign
     * @param l The location of the sign to remove
     * @param removeText If a task should be scheduled to remove the sign text on the next tick
     * @return If the sign was removed or not. If false, the target location was not an ajLeaderboards sign
     */
    public boolean removeSign(Location l, boolean removeText) {
        boolean save = false;
        for(BoardSign s : signs) {
            if(l.equals(s.getLocation())) {
                signs.remove(s);
                save = true;
                s.setRemoved(true);
                if(removeText) Bukkit.getScheduler().runTask(plugin, () -> s.setText("", "", "", ""));
                break;
            }
        }
        if(save) saveFile();
        return save;
    }

    public BoardSign findSign(Location l) {
        for (BoardSign s : signs) {
            if (l.equals(s.getLocation())) {
                return s;
            }
        }
        return null;
    }


    public void addSign(Location loc, String board, int pos, TimedType type) {
        if(findSign(loc) != null) return;
        signs.add(new BoardSign(loc, board, pos, type));
        saveFile();
    }

    public void saveFile() {
        List<String> signsraw = new ArrayList<>();
        for(BoardSign sign : signs) {
            if(sign.isRemoved()) continue;
            signsraw.add(sign.serialize());
        }
        cfg.set("signs", signsraw);
        try {
            cfg.save(cfgFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "An error occurred while saving signs to the file:", e);
        }
    }


    public void updateSigns() {
        updateNameCache();
        for(BoardSign sign : signs) {
            if(sign.isRemoved()) continue;
            updateSign(sign);
        }
    }

    final HashMap<String, String> names = new HashMap<>();
    public void updateNameCache() {
        List<String> namesraw = plugin.getAConfig().getStringList("value-names");
        for(String s : namesraw) {
            if(!s.contains("%")) continue;
            String[] parts = s.split("%");
            names.put(parts[0], parts[1]);
        }
    }

    @SuppressWarnings("unused")
    public Map<String, String> getNames() {
        return new HashMap<>(names);
    }

    public void updateSign(BoardSign sign) {
        if(!isSignChunkLoaded(sign)) return;

        try {
            if(!sign.isPlaced()) return;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            if(plugin.isShuttingDown()) return;
            plugin.getLogger().log(Level.SEVERE, "Interrupted while trying to check if sign is placed:", e);
        }

        String name = "";
        if(names.containsKey(sign.getBoard())) {
            name = names.get(sign.getBoard());
        }

        StatEntry r = plugin.getTopManager().getStat(sign.getPosition(), sign.getBoard(), sign.getType());

        Messages msgs = plugin.getMessages();

        String[] placeholders = Arrays.asList(
                "POSITION:"+sign.getPosition(),
                "NAME:"+r.getPlayerName(),
                "DISPLAYNAME:"+r.getPlayerDisplayName(),
                "VALUE:"+r.getScorePretty(),
                "FVALUE:"+r.getScoreFormatted(),
                "TVALUE:"+r.getTime(),
                "VALUENAME:"+Matcher.quoteReplacement(name),
                "TIMEDTYPE:"+sign.getType().lowerName()
        ).toArray(new String[]{});

        List<Component> lines;
        if(msgs.hasMessage("signs.top."+sign.getBoard())) {
            lines = msgs.getComponentList("signs.top."+sign.getBoard(), placeholders);
        } else {
            lines = msgs.getComponentList("signs.top.default", placeholders);
        }


        List<String> pLines = new ArrayList<>();
        lines.forEach(c -> pLines.add(LEGACY_SIGN_SERIALIZER.serialize(c)));

        if(plugin.isShuttingDown()) return;
        if(r.hasPlayer()) {
            plugin.getHeadManager().search(sign, r.getPlayerName(), r.getPlayerID());
            if(plugin.isShuttingDown()) return;
            plugin.getArmorStandManager().search(sign, r.getPlayerName(), r.getPlayerID());
        }
        if(plugin.isShuttingDown()) return;
        Bukkit.getScheduler().runTask(plugin, () -> sign.setText(pLines.get(0), pLines.get(1), pLines.get(2), pLines.get(3)));
    }

    public boolean isSignChunkLoaded(BoardSign sign) {
        return sign.getWorld().isChunkLoaded(sign.getX(), sign.getZ());
    }

}
