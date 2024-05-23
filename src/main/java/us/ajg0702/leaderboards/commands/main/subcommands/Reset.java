package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class Reset extends SubCommand {

    private final LeaderboardPlugin plugin;
    final HashMap<Object, String> confirmResets = new HashMap<>();

    public Reset(LeaderboardPlugin plugin) {
        super("reset", Collections.emptyList(), null, "Clear all data of a leaderboard");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender ender, String[] args) {
        if(args.length > 1) return Collections.emptyList();
        List<String> boards = new ArrayList<>(plugin.getTopManager().getBoards());
        boards.add("*");
        return filterCompletion(boards, args[0]);
    }


    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a board to reset\n&7Usage: /"+label+" reset <board>"));
            return;
        }
        String board = args[0];
        if(!plugin.getTopManager().boardExists(board) && !board.equals("*")) {
            sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
            return;
        }

        if(!confirmResets.containsKey(sender.getHandle()) || (confirmResets.containsKey(sender.getHandle()) && !confirmResets.get(sender.getHandle()).equals(board))) {
            sender.sendMessage(message("&cThis action will delete data! The top players will have to join again to show up on the leaderboard.\n" +
                    "&cNOTE: &eThis will not reset every player to 0! You need to reset the data in the target placeholder first, then run this command.\n"
                    + "&7Repeat the command within 15 seconds to confirm this action\n" +
                    "&7Or click: <click:run_command:'/"+label+" reset "+board+"'><green><b>" +
                    "<hover:show_text:'<gray>Click to confirm resetting the board\n<red>WARNING: <yellow>This will delete the data for this leaderboard!'>[CONFIRM]</hover>" +
                    "</b></green></click>"));
            confirmResets.put(sender.getHandle(), board);
            Debug.info("Added confirmDelete: "+ confirmResets.keySet().size());
            plugin.getScheduler().runTaskLaterAsynchronously(() -> {
                if(confirmResets.containsKey(sender.getHandle()) && confirmResets.get(sender.getHandle()).equals(board)) {
                    confirmResets.remove(sender.getHandle());
                }

            }, 15*20);
        } else {
            List<String> removingBoards = board.equals("*") ? plugin.getCache().getBoards() : Collections.singletonList(board);
            for (String removingBoard : removingBoards) {
                long start = System.currentTimeMillis();
                confirmResets.remove(sender.getHandle());
                if(!plugin.getCache().removeBoard(removingBoard)) {
                    sender.sendMessage(message("&cSomething went wrong while resetting " + removingBoard + ". Check the console for more info."));
                    return;
                }
                if(!plugin.getCache().createBoard(removingBoard)) {
                    sender.sendMessage(message("&cSomething went wrong while resetting " + removingBoard + ". Check the console for more info."));
                    return;
                }
                sender.sendMessage(message("&aThe board &f" + removingBoard + "&a has been reset!"));
                Debug.info("Reset of " + removingBoard + " took " + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }
}
