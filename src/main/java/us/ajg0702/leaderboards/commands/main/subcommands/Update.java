package us.ajg0702.leaderboards.commands.main.subcommands;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.Collections;
import java.util.List;

public class Update extends SubCommand {
    private final LeaderboardPlugin plugin;

    public Update(LeaderboardPlugin plugin) {
        super("update", Collections.emptyList(), "ajleaderboards.use", "Attempt to manually update a player's stats on the leaderboard");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return null;
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings, String s) {

    }
}
