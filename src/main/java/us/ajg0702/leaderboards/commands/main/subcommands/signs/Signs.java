package us.ajg0702.leaderboards.commands.main.subcommands.signs;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.commands.main.MainCommand;

import java.util.Collections;
import java.util.List;

public class Signs extends SubCommand {

    public Signs(LeaderboardPlugin plugin) {
        super("signs", Collections.emptyList(), "ajleaderboards.use", "Manage leaderboard signs");

        addSubCommand(new AddSign(plugin));
        addSubCommand(new ListSigns(plugin));
        addSubCommand(new RemoveSign(plugin));
        addSubCommand(new Teleport(plugin));
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        if(args.length == 1) {
            return filterCompletion(subCommandAutoComplete(sender, args), args[0]);
        } else {
            return subCommandAutoComplete(sender, args);
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(subCommandExecute(sender, args, label)) return;

        MainCommand.sendHelp(sender, label+" "+getName(), getSubCommands());
    }
}
