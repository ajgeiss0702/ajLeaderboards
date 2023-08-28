package us.ajg0702.leaderboards.commands.main.subcommands.signs;

import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.commands.platforms.bukkit.BukkitSender;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.displays.signs.BoardSign;
import us.ajg0702.utils.spigot.LocUtils;

import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Teleport extends SubCommand {
    private final LeaderboardPlugin plugin;

    public Teleport(LeaderboardPlugin plugin) {
        super("teleport", Collections.singletonList("tp"), null, "Teleport to a signs");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("<red>This command is meant to be used by clicking on signs in <gold>" +
                    "<hover:show_text:Click to run this command><click:RUN_COMMAND:/"+label+" signs list>/"+label+" signs list</click></hover>"));
            return;
        }
        if(!sender.isPlayer()) {
            sender.sendMessage(message("<red>You must do this in game!"));
            return;
        }

        List<BoardSign> signs = plugin.getSignManager().getSigns();

        Player player = (Player) sender.getHandle();

        int signNumber;
        try {
            signNumber = Integer.parseInt(args[0]);
        } catch(NumberFormatException e) {
            sender.sendMessage(message("<red>Invalid sign number!"));
            return;
        }

        if(signNumber >= signs.size()) {
            sender.sendMessage(message("<red>Sign number outside of sign range!"));
            return;
        }

        BoardSign sign = signs.get(signNumber);

        Location loc = sign.getLocation().clone();
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(90f);

        loc.setX(loc.getBlockX()+0.5);
        loc.setZ(loc.getBlockZ()+0.5);

        PaperLib.teleportAsync(player, loc);
    }
}
