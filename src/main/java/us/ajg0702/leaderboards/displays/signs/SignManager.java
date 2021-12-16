package us.ajg0702.leaderboards.displays.signs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.utils.common.Messages;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SignManager {
    private final LeaderboardPlugin plugin;
    private YamlConfiguration cfg;
    private File cfgFile;

    private final List<BoardSign> signs = new CopyOnWriteArrayList<>();

    public SignManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTask(plugin, this::reload);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateSigns, 10*20, 20);
    }

    public List<BoardSign> getSigns() {
        return signs;
    }

    public void reload() {
        cfgFile = new File(plugin.getDataFolder(), "displays.yml");
        cfg = YamlConfiguration.loadConfiguration(cfgFile);
        cfg.options().header("This file is for storing sign location, npcs, and other things in the plugin that might display data");

        signs.clear();
        if(cfg.contains("signs")) {
            List<String> rawsigns = cfg.getStringList("signs");
            for(String s : rawsigns) {
                try {
                    signs.add(BoardSign.deserialize(s));
                } catch(Exception e) {
                    plugin.getLogger().warning("An error occurred while loading a sign:");
                    e.printStackTrace();
                }
            }
        }
        updateNameCache();
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
        List<String> namesraw = plugin.getAConfig().getStringList("value-names");
        for(String s : namesraw) {
            if(!s.contains("%")) continue;
            String[] parts = s.split("%");
            names.put(parts[0], parts[1]);
        }
    }

    public void updateSign(BoardSign sign) {
        if(!isSignChunkLoaded(sign)) return;

        String name = "";
        if(names.containsKey(sign.getBoard())) {
            name = names.get(sign.getBoard());
        }

        StatEntry r = plugin.getTopManager().getStat(sign.getPosition(), sign.getBoard(), sign.getType());

        Messages msgs = plugin.getMessages();

        List<String> lines = Arrays.asList(
                msgs.getString("signs.top.1"),
                msgs.getString("signs.top.2"),
                msgs.getString("signs.top.3"),
                msgs.getString("signs.top.4"));


        List<String> plines = new ArrayList<>();
        for(String l : lines) {
            String pline = l
                    .replaceAll("\\{POSITION}", sign.getPosition()+"")
                    .replaceAll("\\{NAME}", r.getPlayer())
                    .replaceAll("\\{VALUE}", r.getScorePretty())
                    .replaceAll("\\{VALUENAME}", name)
                    ;
            plines.add(pline);

        }

        if(r.hasPlayer()) {
            plugin.getHeadManager().search(sign, r.getPlayer(), r.getPlayerID());
            plugin.getArmorStandManager().search(sign, r.getPlayer(), r.getPlayerID());
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            sign.setText(plines.get(0), plines.get(1), plines.get(2), plines.get(3));
        });
    }

    public boolean isSignChunkLoaded(BoardSign sign) {
        return sign.getWorld().isChunkLoaded(sign.getX(), sign.getZ());
    }

}
