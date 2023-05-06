package us.ajg0702.leaderboards.commands.main.subcommands.debug;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.boards.StatEntry;

import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Format extends SubCommand {
    public Format() {
        super("format", Collections.emptyList(), "ajleaderboards.use", null);
        setShowInTabComplete(false);
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        sender.sendMessage(
                message(
                        StatEntry.formatDouble(Double.parseDouble(args[0]))
                )
        );
    }
}
