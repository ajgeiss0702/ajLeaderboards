package us.ajg0702.leaderboards.commands.main.subcommands.signs;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.displays.signs.BoardSign;
import us.ajg0702.utils.spigot.LocUtils;

import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class ListSigns extends SubCommand {
    private final LeaderboardPlugin plugin;

    public ListSigns(LeaderboardPlugin plugin) {
        super("list", Collections.emptyList(), null, "List all created signs");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        List<BoardSign> signs = plugin.getSignManager().getSigns();
        StringBuilder s = new StringBuilder("&6Signs");
        for(BoardSign sign : signs) {
            s.append("\n&7- &e").append(LocUtils.locToString(sign.getLocation())).append(" ").append(sign.getBoard());
        }
        if(signs.size() == 0) {
            s.append("\n&7None");
        }
        sender.sendMessage(message(s.toString()));
    }
}
