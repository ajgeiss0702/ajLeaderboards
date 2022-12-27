package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class UpdatePlayer extends SubCommand {
    private final LeaderboardPlugin plugin;

    public UpdatePlayer(LeaderboardPlugin plugin) {
        super("updateplayer", Collections.emptyList(), "ajleaderboards.use", "Attempt to manually update a player's stats on the leaderboard");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] args) {
        if(args.length == 2) return null;
        if(args.length == 1) {
            return plugin.getTopManager().getBoards();
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 2) {
            sender.sendMessage(message("&cPlease provide a board and player to update\n&7Usage: /"+label+" update <board> <player>"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String board = args[0];
            if(!plugin.getCache().boardExists(board)) {
                sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
                return;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);
            plugin.getCache().updateStat(board, p);

            boolean attemptHasWarning = CheckUpdate.checkUpdate(board, p, plugin, sender);
            sender.sendMessage(message("&"+ (attemptHasWarning ? "e" : "a") +"Attempted to update stat for "+p.getName()+" on board "+board));
        });
    }
}
