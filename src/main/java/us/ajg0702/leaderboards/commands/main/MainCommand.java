package us.ajg0702.leaderboards.commands.main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import us.ajg0702.commands.BaseCommand;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.commands.main.subcommands.Version;

import java.util.Arrays;
import java.util.List;

public class MainCommand extends BaseCommand {
    private final LeaderboardPlugin plugin;
    public MainCommand(LeaderboardPlugin plugin) {
        super("ajleaderboards", Arrays.asList("ajl", "ajlb"), "ajleaderboards.use", "Main comamnd for ajLeaderboards");
        this.plugin = plugin;

        addSubCommand(new Version(plugin));
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        return subCommandAutoComplete(sender, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(!checkPermission(sender)) {
            sender.sendMessage(message("<red>You don't have permission to do this!"));
        }
        if(subCommandExecute(sender, args, label)) return;



        sender.sendMessage(message("hello"));
    }

    public static Component message(String miniMessage) {
        return MiniMessage.get().parse(miniMessage);
    }
}
