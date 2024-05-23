package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.Debug;
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
    public List<String> autoComplete(CommandSender commandSender, String[] args) {
        if(args.length == 1) return filterCompletion(plugin.getTopManager().getBoards(), args[0]);
        return Collections.emptyList();
    }

    final HashMap<Object, String> confirmDeletes = new HashMap<>();

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a placeholder to remove.\n&7Usage: /"+label+" remove <board>"));
            return;
        }
        String board = args[0];
        if(!plugin.getCache().boardExists(board)) {
            sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
            return;
        }

        Debug.info("Confirming: "+confirmDeletes.containsKey(sender));

        if(!confirmDeletes.containsKey(sender.getHandle()) || (confirmDeletes.containsKey(sender.getHandle()) && !confirmDeletes.get(sender.getHandle()).equals(board))) {
            sender.sendMessage(message("&cThis action will delete data! If you add back the board, the top players will have to join again to show up.\n"
                    + "&7Repeat the command within 15 seconds to confirm this action\n" +
                    "&7Or click: <click:run_command:'/ajleaderboards remove "+board+"'><green><b>" +
                        "<hover:show_text:'<gray>Click to confirm removing the board\n<red>WARNING: <yellow>This will delete the cache for this leaderboard!'>[CONFIRM]</hover>" +
                    "</b></green></click>"));
            confirmDeletes.put(sender.getHandle(), board);
            Debug.info("Added confirmDelete: "+confirmDeletes.keySet().size());
            plugin.getScheduler().runTaskLaterAsynchronously(() -> {
                Debug.info("Removing confirmDelete");
                if(confirmDeletes.containsKey(sender.getHandle()) && confirmDeletes.get(sender.getHandle()).equals(board)) {
                    confirmDeletes.remove(sender.getHandle());
                    Debug.info("Removed confirmDelete");
                }

            }, 15*20);
        } else {
            confirmDeletes.remove(sender);
            if(plugin.getCache().removeBoard(board)) {
                sender.sendMessage(message("&aThe board has been removed!"));
            } else {
                sender.sendMessage(message("&cSomething went wrong. Check the console for more info."));
            }

        }
    }
}
