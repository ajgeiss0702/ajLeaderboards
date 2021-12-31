package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.utils.spigot.Messages;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemovePlayer extends SubCommand {

    private final LeaderboardPlugin plugin;

    public RemovePlayer(LeaderboardPlugin plugin) {
        super("removeplayer", Arrays.asList("rmplayer", "rmpl"), "ajleaderboards.use", "Clear a player from the cache. If they are not excluded from the leaderboard, they will be added next time they are updated.");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        List<String> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(p -> {
            players.add(p.getName());
        });
        return players;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length <= 1) {
            sender.sendMessage(message("&cPlease provide a board and a player.\n&7Usage: /"+label+" removeplayer <player> <board>"));
            return;
        }
        if(args.length <= 2) {
            sender.sendMessage(message("&cPlease provide a board.\n&7Usage: /"+label+" removeplayer <player> <board>"));
            return;
        }
        String playername = args[1];
        String board = args[2];
        if(!plugin.getCache().getBoards().contains(board)) {
            sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if(plugin.getCache().removePlayer(board, playername)) {
                sender.sendMessage(message("&aRemoved "+playername+" from "+board+"!"));
            } else {
                sender.sendMessage(message("&cUnable to remove "+playername+" from "+board+". &7Check the console for more info."));
            }

        });
    }
}
