package us.ajg0702.leaderboards.commands.main.subcommands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.helpers.DbRow;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Export extends SubCommand {
    private final LeaderboardPlugin plugin;
    public Export(LeaderboardPlugin plugin) {
        super("export", Collections.emptyList(), null, "Export cache to a .json file that can be imported later");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        if(args.length == 1 && !args[0].isEmpty()) {
            return Collections.singletonList(addJsonEnding(args[0]));
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a file name.\n&7Usage: /"+label+" export <file>"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String fileName = addJsonEnding(args[0]);
            File file = new File(plugin.getDataFolder(), fileName);

            if(file.exists()) {
                sender.sendMessage(plugin.getMessages().getComponent("commands.export.fileexists", "FILE:"+fileName));
                return;
            }

            JsonObject obj = plugin.getExporter().export(sender);
            if(obj == null) {
                sender.sendMessage(plugin.getMessages().getComponent("commands.export.fail"));
                return;
            }

            try {
                Writer writer = new FileWriter(file);
                new Gson().toJson(obj, writer);
                writer.close();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while writing:", e);
                sender.sendMessage(plugin.getMessages().getComponent("commands.export.fail"));
                return;
            }

            sender.sendMessage(plugin.getMessages().getComponent("commands.export.success", "FILE:"+fileName));
            plugin.getLogger().info("Exporting to file "+fileName+" complete!");
        });
    }

    private String addJsonEnding(String has) {
        if(has.endsWith(".json")) {
            return has;
        }
        int lastDot = has.lastIndexOf('.');
        if(lastDot != -1) {
            String afterDot = has.substring(lastDot);
            if(recursiveEquals(afterDot) && afterDot.length() < 5) {
                return has+(".json".substring(afterDot.length()));
            } else {
                return has+".json";
            }
        } else {
            return has+".json";
        }
    }

    private boolean recursiveEquals(String thing) {
        for (int i = ".json".length(); i >= 0; i--) {
            String s = ".json".substring(0, i);
            if(thing.equals(s)) return true;
        }
        return false;
    }
}
