package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.utils.OfflineUpdater;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class UpdateAllOffline extends SubCommand {
    private final LeaderboardPlugin plugin;
    public UpdateAllOffline(LeaderboardPlugin plugin) {
        super(
                "updatealloffline",
                Collections.emptyList(),
                "ajleaderboards.use",
                "Attempts to update all offline players on the specified board."
        );
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        if(args.length < 2) {
            return plugin.getTopManager().getBoards();
        }
        return Arrays.asList("start", "progress");
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(!checkPermission(sender)) {
            sender.sendMessage(plugin.getMessages().getComponent("noperm"));
            return;
        }

        if(args.length < 1) {
            sender.sendMessage(message(
                    "&cPlease provide a board.\n" +
                            "&7Usage: /" + label + " updatealloffline <board> [start/progress]"
            ));
            return;
        }

        String board = args[0];
        if(!plugin.getTopManager().boardExists(board)) {
            sender.sendMessage(message("&cThat board does not exist!"));
            return;
        }

        if(args.length < 2 || args[1].equalsIgnoreCase("start")) {
            if(plugin.getOfflineUpdaters().containsKey(board)) {
                sender.sendMessage(message(
                        "&cThat board is already being processed.\n" +
                                "&7To check it's progress, use &f/" + label + " updatealloffline " + board + " progress"
                ));
                return;
            }
            OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
            plugin.getOfflineUpdaters().put(board, new OfflineUpdater(plugin, board, offlinePlayers, sender));
            sender.sendMessage(message(
                    "&aStarted update of &f" + offlinePlayers.length + " &aplayers!\n" +
                            "&7You can check the progress by either checking the console, or running " +
                            "&f/" + label + " updatealloffline " + board + " progress\n" +
                            "&6Reminder: &7Not all placeholders support updating offline players, so if nothing is updated, it could be the fault of the placeholder, not ajLeaderboards."
            ));
        } else if(args[1].equalsIgnoreCase("progress")) {
            if(!plugin.getOfflineUpdaters().containsKey(board)) {
                sender.sendMessage(message(
                        "&cThat board is not being processed\n" +
                                "&7Start one using &f/" + label + " updatealloffline " + board + " start"
                ));
                return;
            }

            OfflineUpdater offlineUpdater = plugin.getOfflineUpdaters().get(board);
            sender.sendMessage(message(
                    "&6" + board + "&e: &6" +
                            Math.round(offlineUpdater.getProgressPercent() * 1000)/10 + "&e% done " +
                            "(&6" + offlineUpdater.getRemainingPlayers() + " &e/&6 " + offlineUpdater.getStarted() + "&e)"
            ));
        } else {
            sender.sendMessage(message(
                    "&cInvalid argument!\n" +
                            "&7Usage: /" + label + " updatealloffline <board> [start/progress]"
            ));
            return;
        }
    }
}
