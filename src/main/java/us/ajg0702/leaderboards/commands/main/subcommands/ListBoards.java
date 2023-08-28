package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Collections;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class ListBoards extends SubCommand {
    private final LeaderboardPlugin plugin;

    public ListBoards(LeaderboardPlugin plugin) {
        super("list", Collections.emptyList(), "ajleaderboards.use", "List all boards in ajleaderboards, or list the top 10 players in a certain board.");
        this.plugin = plugin;
    }

    @Override
    public java.util.List<String> autoComplete(CommandSender commandSender, String[] args) {
        return filterCompletion(plugin.getTopManager().getBoards(), args[0]);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        plugin.getScheduler().runTaskAsynchronously(() -> {
            if(args.length < 1) {
                StringBuilder list = new StringBuilder("&6Boards");
                for(String boardn : plugin.getTopManager().getBoards()) {
                    list.append("\n&7- &e").append(boardn);
                }
                sender.sendMessage(message(list.toString()));
                return;
            }
            String boardn = args[0];
            if(!plugin.getCache().boardExists(boardn)) {
                sender.sendMessage(message("&cThe board '"+boardn+"' does not exist."));
                return;
            }
            StringBuilder list = new StringBuilder("&6Top for " + boardn);
            for(int i = 1;i<=10;i++) {
                StatEntry e = plugin.getCache().getStat(i, boardn, TimedType.ALLTIME);
                list.append("\n&6").append(i).append(". &e").append(e.getPlayerName()).append(" &7- &e").append(e.getScorePretty());
            }
            sender.sendMessage(message(list.toString()));
        });
    }
}
