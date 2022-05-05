package us.ajg0702.leaderboards.commands.main.subcommands;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.commands.main.MainCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Version extends SubCommand {
    private final LeaderboardPlugin plugin;
    public Version(LeaderboardPlugin plugin) {
        super("version", Collections.singletonList("ver"), null, "Show the version of ajLeaderboards");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        sender.sendMessage(message(
                "<gold>ajLedeaderboards v<yellow>"+plugin.getDescription().getVersion()+"<gold> by ajgeiss0702"
        ));
    }
}
