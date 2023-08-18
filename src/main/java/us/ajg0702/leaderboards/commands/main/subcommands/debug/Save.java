package us.ajg0702.leaderboards.commands.main.subcommands.debug;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Save extends SubCommand {

    private final LeaderboardPlugin plugin;

    public Save(LeaderboardPlugin plugin) {
        super("save", Collections.emptyList(), "ajleaderboards.debug", "");
        this.plugin = plugin;
        setShowInTabComplete(false);
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        if(args.length == 1) {
            return filterCompletion(plugin.getTopManager().getBoards(), args[0]);
        }
        if(args.length == 2) {
            return filterCompletion(TimedType.lowerNames(), args[1]);
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 2) {
            sender.sendMessage(message("&cNot enough args! board, type"));
            return;
        }
        plugin.getResetSaver().save(args[0], TimedType.valueOf(args[1].toUpperCase(Locale.ROOT)));
        sender.sendMessage(message("&adid it"));
    }
}
