package us.ajg0702.leaderboards.commands.main.subcommands.signs;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.displays.signs.BoardSign;
import us.ajg0702.utils.spigot.LocUtils;

import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class RemoveSign extends SubCommand {
    private final LeaderboardPlugin plugin;

    public RemoveSign(LeaderboardPlugin plugin) {
        super("remove", Collections.emptyList(), null, "Remove the sign youre looking at");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(!sender.isPlayer()) {
            sender.sendMessage(message("&cYou must be in-game to do this!"));
            return;
        }
        Player player = (Player) sender.getHandle();
        Block target = player.getTargetBlock(null, 10);
        if(!target.getType().toString().contains("SIGN")) {
            sender.sendMessage(message("&cThe block you are looking at is not a sign! Please look at a sign to remove."));
            return;
        }
        if(plugin.getSignManager().removeSign(target.getLocation())) {
            sender.sendMessage(message("&aSign removed!"));
        } else {
            sender.sendMessage(message("&cThat is not an ajLeaderboards sign!"));
        }
    }
}

