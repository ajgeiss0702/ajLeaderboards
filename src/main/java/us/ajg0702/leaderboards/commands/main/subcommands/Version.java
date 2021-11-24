package us.ajg0702.leaderboards.commands.main.subcommands;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.commands.main.MainCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Version extends SubCommand {
    private final LeaderboardPlugin plugin;
    public Version(LeaderboardPlugin plugin) {
        super("version", Collections.emptyList(), null, "Show the version of ajLeaderboards");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return new ArrayList<>();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        sender.sendMessage(MainCommand.message(
                "<gold>ajLedeaderboards v<yellow>"+plugin.getDescription().getVersion()+"<gold> by ajgeiss0702"
        ));
    }
}
