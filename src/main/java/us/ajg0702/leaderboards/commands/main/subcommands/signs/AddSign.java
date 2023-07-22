package us.ajg0702.leaderboards.commands.main.subcommands.signs;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class AddSign extends SubCommand {
    private final LeaderboardPlugin plugin;

    public AddSign(LeaderboardPlugin plugin) {
        super("add", Collections.emptyList(), null, "Create a leaderboard sign at the sign you are looking at");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] args) {
        if(args.length == 1) {
            return filterCompletion(plugin.getTopManager().getBoards(), args[0]);
        }
        if(args.length == 3) {
            return filterCompletion(TimedType.lowerNames(), args[2]);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final String usage = "signs add <board> <position> <type>";

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a board and a position for the sign!\n&7Usage: /"+label+" "+usage));
            return;
        }
        if(args.length < 2) {
            sender.sendMessage(message("&cPlease provide a position for the sign\n&7Usage: /"+label+" "+usage));
            return;
        }
        if(args.length < 3) {
            sender.sendMessage(message("&cPlease provide a timed type for the sign\n&7Usage: /"+label+" "+usage));
            return;
        }
        if(!sender.isPlayer()) {
            sender.sendMessage(message("&cYou must be in-game to do this!"));
            return;
        }
        Player p = (Player) sender.getHandle();
        int pos;
        try {
            pos = Integer.parseInt(args[1]);
        } catch(NumberFormatException e) {
            sender.sendMessage(message("&cInvalid number! Please enter a real number for the position.\n&7Usage: /"+label+" signs add <board> <position>"));
            return;
        }
        Block target = p.getTargetBlock(null, 10);
        if(!target.getType().toString().contains("SIGN")) {
            sender.sendMessage(message("&cThe block you are looking at (" + target.getType() + ") is not a sign! Please look at a sign to set."));
            return;
        }
        TimedType type;
        try {
            type = TimedType.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch(IllegalArgumentException e) {
            StringBuilder list = new StringBuilder();
            TimedType.lowerNames().forEach(s -> list.append(s).append(", "));
            list.delete(list.length()-2, list.length()-1);
            sender.sendMessage(message("&cInvalid timed type!\n&7Options: "+list));
            return;
        }
        if(!plugin.getTopManager().boardExists(args[0])) {
            sender.sendMessage(message("&cInvalid board name! &7Make sure to &cnot&7 put &f%&7 around the board name, and also make sure that you created the board.\n&7If you havent yet created the board, there are instructions here:<reset> https://wiki.ajg0702.us/ajleaderboards/setup/setup/ &7(make sure to not skip steps 1 and 2!)"));
            return;
        }
        plugin.getSignManager().addSign(target.getLocation(), args[0], pos, type);
        sender.sendMessage(message("&aSign created!"));
    }
}
