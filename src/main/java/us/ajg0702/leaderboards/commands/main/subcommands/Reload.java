package us.ajg0702.leaderboards.commands.main.subcommands;

import org.spongepowered.configurate.ConfigurateException;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.Collections;
import java.util.List;

public class Reload extends SubCommand {

    private final LeaderboardPlugin plugin;

    public Reload(LeaderboardPlugin plugin) {
        super("reload", Collections.emptyList(), "ajleaderboards.use", "Reload the config files");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(!checkPermission(sender)) {
            sender.sendMessage(plugin.getMessages().getComponent("noperm"));
            return;
        }


        try {
            plugin.getAConfig().reload();
            plugin.getMessages().reload();
            plugin.getSignManager().reload();

            plugin.reloadInterval();
            Debug.setDebug(plugin.getAConfig().getBoolean("debug"));
        } catch (ConfigurateException e) {
            e.printStackTrace();
            sender.sendMessage(plugin.getMessages().getComponent("commands.reload.fail"));
        }


        sender.sendMessage(plugin.getMessages().getComponent("commands.reload.success"));
    }
}
