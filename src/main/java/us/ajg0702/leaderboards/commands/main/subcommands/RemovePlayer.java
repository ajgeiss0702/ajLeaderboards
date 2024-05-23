package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class RemovePlayer extends SubCommand {

    private final LeaderboardPlugin plugin;

    public RemovePlayer(LeaderboardPlugin plugin) {
        super("removeplayer", Arrays.asList("rmplayer", "rmpl"), "ajleaderboards.use", "Clear a player from the cache. If they are not excluded from the leaderboard, they will be added next time they are updated.");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] args) {
        if(args.length == 1) return null;
        if(args.length == 2) {
            List<String> boards = new ArrayList<>(plugin.getTopManager().getBoards());
            boards.add("*");
            return filterCompletion(boards, args[1]);
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a board and a player.\n&7Usage: /"+label+" removeplayer <player> <board>"));
            return;
        }
        if(args.length < 2) {
            sender.sendMessage(message("&cPlease provide a board.\n&7Usage: /"+label+" removeplayer <player> <board>"));
            return;
        }
        String playername = args[0];
        String board = args[1];
        if(!plugin.getCache().boardExists(board) && !"*".equals(board)) {
            sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
            return;
        }
        List<String> boards = Collections.singletonList(board);
        if("*".equals(board)) {
            boards = plugin.getCache().getBoards();
        }
        List<String> finalBoards = boards;
        plugin.getScheduler().runTaskAsynchronously(() -> {
            for(String b : finalBoards) {
                if(plugin.getCache().removePlayer(b, playername)) {
                    sender.sendMessage(message("&aRemoved "+playername+" from "+b+"!"));
                } else {
                    sender.sendMessage(message("&cUnable to remove "+playername+" from "+b+". &7Check the console for more info."));
                }
            }
            if("*".equals(board)) {
                sender.sendMessage(message("&aFinished removing "+playername+" from all boards!"));
            }
        });
    }
}
