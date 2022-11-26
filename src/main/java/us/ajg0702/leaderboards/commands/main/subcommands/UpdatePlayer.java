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
            plugin.getCache().updateStat(args[0], p);

            List<String> updatableBoards = plugin.getAConfig().getStringList("only-update");
            boolean attemptHasWarning = !p.isOnline() || p.getPlayer() != null && (plugin.getAConfig().getBoolean("enable-dontupdate-permission") && p.getPlayer().hasPermission("ajleaderboards.dontupdate."+args[0]));
            if(!p.isOnline()) {
                sender.sendMessage(message("&6Warning: &7The player you requested to update appears to be offline. Not all placeholders support this. I'll still try, but if there is an error or nothing is updated, the placeholder probably doesn't support it."));
            } else if(p.getPlayer() != null && (plugin.getAConfig().getBoolean("enable-dontupdate-permission") && p.getPlayer().hasPermission("ajleaderboards.dontupdate."+args[0]))) {
                sender.sendMessage(message(
                        "&6Warning: &7The player you requested to update has the &fdontupdate&7 permission. This player will not be automatically updated!\n" +
                                "<hover:show_text:'<yellow>Click to go to https://wiki.ajg0702.us/ajleaderboards/faq#admins-dont-show-up-on-the-leaderboard'>" +
                                "<click:open_url:'https://wiki.ajg0702.us/ajleaderboards/faq#admins-dont-show-up-on-the-leaderboard'>" +
                                "<white><underlined>Read more on the wiki (click me)" +
                                "</click>" +
                                "</hover>\n"
                ));
            } else if(!plugin.getAConfig().getBoolean("update-stats")) {
                attemptHasWarning = true;
                sender.sendMessage(message(
                        "&6Warning: &7Updating all boards is disabled in the config! Nobody will be automatically updated on this server!\n" +
                                "\n" +
                                "&7If this is unintended, enable &fupdate-stats&7 in the config!\n"
                ));
            } else if(!updatableBoards.isEmpty() && !updatableBoards.contains(args[0])) {
                attemptHasWarning = true;
                sender.sendMessage(message(
                        "&6Warning: &7Updating this board is disabled in the config! Nobody will be automatically updated (for this board) on this server!\n" +
                                "\n" +
                                "&7If this is unintended, add &f" + args[0] + "&7 to &fonly-update&7 in the config, or set it to &f[]\n"
                ));
            }
            sender.sendMessage(message("&"+ (attemptHasWarning ? "e" : "a") +"Attempted to update stat for "+p.getName()+" on board "+args[0]));
        });
    }
}
