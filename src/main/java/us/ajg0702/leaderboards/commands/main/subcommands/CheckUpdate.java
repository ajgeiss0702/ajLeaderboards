package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.Collections;
import java.util.List;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class CheckUpdate extends SubCommand {
    private final LeaderboardPlugin plugin;
    public CheckUpdate(LeaderboardPlugin plugin) {
        super("checkupdate", Collections.singletonList("updatecheck"), null, "Check if there could be something wrong with automatically updating a certain player");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        if(args.length == 2) return null;
        if(args.length == 1) {
            return plugin.getTopManager().getBoards();
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 2) {
            sender.sendMessage(message("&cPlease provide a board and player to check\n&7Usage: /"+label+" checkupdate <board> <player>"));
            return;
        }
        String board = args[0];
        if(!plugin.getCache().boardExists(board)) {
            sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
            return;
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer p = Bukkit.getOfflinePlayer(args[1]);

        boolean hasWarnings = checkUpdate(board, p, plugin, sender);
        if(hasWarnings) {
            sender.sendMessage(message("&eCheck the above warnings for why that player might not be automatically updated"));
        } else {
            sender.sendMessage(message("&aThat player should be automatically updated for that board!"));
        }
    }

    public static boolean checkUpdate(String board, OfflinePlayer player, LeaderboardPlugin plugin, CommandSender sender) {
        List<String> updatableBoards = plugin.getAConfig().getStringList("only-update");
        boolean attemptHasWarning = false;

        if(!player.isOnline()) {
            attemptHasWarning = true;
            sender.sendMessage(message("&6Warning: &7That player appears to be offline. Not all placeholders support updating while the player is offline. You might still be able to update them manually, but if nothing happens then the placeholder probably doesn't support it."));
        } else if(player.getPlayer() != null && (plugin.getAConfig().getBoolean("enable-dontupdate-permission") && player.getPlayer().hasPermission("ajleaderboards.dontupdate."+board))) {
            attemptHasWarning = true;
            sender.sendMessage(message(
                    "&6Warning: &7That player has the &fdontupdate&7 permission. This player will not be automatically updated!\n" +
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
        } else if(!updatableBoards.isEmpty() && !updatableBoards.contains(board)) {
            attemptHasWarning = true;
            sender.sendMessage(message(
                    "&6Warning: &7Updating this board is disabled in the config! Nobody will be automatically updated (for this board) on this server!\n" +
                            "\n" +
                            "&7If this is unintended, add &f" + board + "&7 to &fonly-update&7 in the config, or set it to &f[]\n"
            ));
        }
        return attemptHasWarning;
    }
}
