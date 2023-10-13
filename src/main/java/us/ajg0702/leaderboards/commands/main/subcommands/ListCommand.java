package us.ajg0702.leaderboards.commands.main.subcommands;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Collections;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class ListCommand extends SubCommand {
    private final LeaderboardPlugin plugin;

    public ListCommand(LeaderboardPlugin plugin) {
        super("list", Collections.emptyList(), "ajleaderboards.use", "List all boards in ajleaderboards, or list the top 10 players in a certain board.");
        this.plugin = plugin;
    }

    @Override
    public java.util.List<String> autoComplete(CommandSender commandSender, String[] args) {
        if(args.length <= 1) {
            return filterCompletion(plugin.getTopManager().getBoards(), args[0]);
        } else if(args.length == 2) {
            return filterCompletion(TimedType.lowerNames(), args[1]);
        } else {
            return Collections.emptyList();
        }
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
            String board = args[0];
            TimedType timedType = args.length > 1 ? TimedType.of(args[1]) : TimedType.ALLTIME;
            if(!plugin.getCache().boardExists(board)) {
                sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
                return;
            }
            StringBuilder list = new StringBuilder("&6Top for " + board + " " + timedType.lowerName());
            for(int i = 1;i<=10;i++) {
                StatEntry e = plugin.getCache().getStat(i, board, timedType);
                list.append("\n&6").append(i).append(". &e").append(e.getPlayerName()).append(" &7- &e").append(e.getScorePretty());
            }
            sender.sendMessage(message(list.toString()));
        });
    }
}
