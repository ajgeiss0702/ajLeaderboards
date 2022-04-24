package us.ajg0702.leaderboards.commands.main.subcommands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.helpers.DbRow;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Import extends SubCommand {
    private final LeaderboardPlugin plugin;
    public Import(LeaderboardPlugin plugin) {
        super("import", Collections.emptyList(), null, "Import cache data from a file");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        if(args.length != 1) return Collections.emptyList();
        List<String> fileNames = new ArrayList<>();
        File[] files = plugin.getDataFolder().listFiles((dir, name) -> name.endsWith(".json"));
        if(files == null) return Collections.emptyList();
        for(File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a file name.\n&7Usage: /"+label+" import <file>"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), args[0]);
            if(!file.exists()) {
                sender.sendMessage(plugin.getMessages().getComponent("commands.import.nofile", "FILE:"+file.getName()));
                return;
            }
            sender.sendMessage(plugin.getMessages().getComponent("commands.import.starting", "FILE:"+file.getName()));
            try {
                Gson gson = new Gson();
                Reader fileReader = new FileReader(file);
                JsonObject object = gson.fromJson(fileReader, JsonObject.class);
                Set<String> boards = object.keySet();

                int i = 0;
                for(String board : boards) {
                    if(!plugin.getCache().boardExists(board)) {
                        plugin.getCache().createBoard(board);
                    }

                    Debug.info("Importing "+board);

                    List<DbRow> rows = new ArrayList<>();
                    JsonArray jsonRowList = object.getAsJsonArray(board);
                    for(JsonElement element : jsonRowList) {
                        Debug.info(gson.toJson(element));
                        rows.add(DbRow.fromJsonObject(element.getAsJsonObject()));
                    }

                    plugin.getCache().insertRows(board, rows);
                    sender.sendMessage(plugin.getMessages().getComponent("commands.import.insertprogress", "DONE:"+ ++i, "TOTAL:"+boards.size()));
                }

                fileReader.close();

                sender.sendMessage(plugin.getMessages().getComponent("commands.import.success", "FILE:"+file.getName()));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error while importing cache:", e);
                sender.sendMessage(plugin.getMessages().getComponent("commands.import.fail"));
            }
        });
    }
}
