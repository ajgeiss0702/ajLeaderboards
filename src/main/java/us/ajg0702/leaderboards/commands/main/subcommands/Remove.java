package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Remove extends SubCommand {
    private final LeaderboardPlugin plugin;

    public Remove(LeaderboardPlugin plugin) {
        super("remove", Collections.emptyList(), "ajleaderboards.use", "Delete a board and all of its data.");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return plugin.getCache().getBoards();
    }

    HashMap<CommandSender, String> confirmDeletes = new HashMap<>();

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length <= 1) {
            sender.sendMessage(message("&cPlease provide a placeholder to remove.\n&7Usage: /"+label+" remove <board>"));
            return;
        }
        String board1 = args[1];
        if(!plugin.getCache().getBoards().contains(board1)) {
            sender.sendMessage(message("&cThe board '"+board1+"' does not exist."));
            return;
        }

        if(!confirmDeletes.containsKey(sender) || (confirmDeletes.containsKey(sender) && !confirmDeletes.get(sender).equals(board1))) {
            sender.sendMessage(message("&cThis action will delete data! If you add back the board, the top players will have to join again to show up.\n"
                    + "&7Repeat the command within 30 seconds to confirm this action"));
            confirmDeletes.put(sender, board1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if(confirmDeletes.containsKey(sender) && confirmDeletes.get(sender).equals(board1)) {
                    confirmDeletes.remove(sender);
                }
            }, 30*20);
        } else {
            confirmDeletes.remove(sender);
            if(plugin.getCache().removeBoard(board1)) {
                sender.sendMessage(message("&aThe board has been removed!"));
            } else {
                sender.sendMessage(message("&cSomething went wrong. Check the console for more info."));
            }

        }
    }
}
