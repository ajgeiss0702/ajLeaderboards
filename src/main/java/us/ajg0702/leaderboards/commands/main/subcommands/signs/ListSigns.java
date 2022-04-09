package us.ajg0702.leaderboards.commands.main.subcommands.signs;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
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
        int i = 0;
        for(BoardSign sign : signs) {
            s.append("\n&7- &e")
                    .append("<hover:show_text:Click to teleport to this sign>")
                    .append("<click:RUN_COMMAND:/").append(label).append(" signs tp ").append(i++).append(">")
                    .append(locToString(sign.getLocation())).append(" ").append(sign.getBoard())
                    .append(" ").append(sign.getType().lowerName())
                    .append("</click>")
                    .append("</hover>")
            ;
        }
        if(signs.size() == 0) {
            s.append("\n&7None");
        }
        sender.sendMessage(message(s.toString()));
    }

    private static String locToString(@Nullable Location l) {
        if(l == null) return null;
        if(l.getWorld() == null) return null;
        return l.getWorld().getName() + ", " + l.getX() + ", " + l.getY() + ", " + l.getZ();
    }
}
