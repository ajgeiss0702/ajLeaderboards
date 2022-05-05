package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;

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
        return plugin.getTopManager().getBoards();
    }


    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a board to reset\n&7Usage: /"+label+" reset <board>"));
            return;
        }
        String board = args[0];
        if(!plugin.getTopManager().boardExists(board)) {
            sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
            return;
        }

        if(!confirmResets.containsKey(sender.getHandle()) || (confirmResets.containsKey(sender.getHandle()) && !confirmResets.get(sender.getHandle()).equals(board))) {
            sender.sendMessage(message("&cThis action will delete data! The top players will have to join again to show up  on the leaderboard.\n"
                    + "&7Repeat the command within 15 seconds to confirm this action\n" +
                    "&7Or click: <click:run_command:'/"+label+" reset "+board+"'><green><b>" +
                    "<hover:show_text:'<gray>Click to confirm resetting the board\n<red>WARNING: <yellow>This will delete the data for this leaderboard!'>[CONFIRM]</hover>" +
                    "</b></green></click>"));
            confirmResets.put(sender.getHandle(), board);
            Debug.info("Added confirmDelete: "+ confirmResets.keySet().size());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if(confirmResets.containsKey(sender.getHandle()) && confirmResets.get(sender.getHandle()).equals(board)) {
                    confirmResets.remove(sender.getHandle());
                }

            }, 15*20);
        } else {
            confirmResets.remove(sender.getHandle());
            if(!plugin.getCache().removeBoard(board)) {
                sender.sendMessage(message("&cSomething went wrong. Check the console for more info."));
                return;
            }
            if(!plugin.getCache().createBoard(board)) {
                sender.sendMessage(message("&cSomething went wrong. Check the console for more info."));
                return;
            }
            sender.sendMessage(message("&aThe board has been reset!"));
        }
    }
}
