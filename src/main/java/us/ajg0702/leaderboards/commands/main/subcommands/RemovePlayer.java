package us.ajg0702.leaderboards.commands.main.subcommands;

import org.bukkit.Bukkit;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static us.ajg0702.leaderboards.LeaderboardPlugin.message;

public class RemovePlayer extends SubCommand {

    private final LeaderboardPlugin plugin;

    public RemovePlayer(LeaderboardPlugin plugin) {
        super("removeplayer", Arrays.asList("rmplayer", "rmpl"), "ajleaderboards.use", "Clear a player from the cache. If they are not excluded from the leaderboard, they will be added next time they are updated. Accepts either a username or a UUID.");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] args) {
        if(args.length == 1) return null;
        if(args.length == 2) {
            List<String> boards = new ArrayList<>(plugin.getTopManager().getBoards());
            boards.add("*");
            return filterCompletion(boards, args[1]);
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length < 1) {
            sender.sendMessage(message("&cPlease provide a player and a board.\n&7Usage: /"+label+" removeplayer <player/uuid> <board>"));
            return;
        }
        if(args.length < 2) {
            sender.sendMessage(message("&cPlease provide a board.\n&7Usage: /"+label+" removeplayer <player/uuid> <board>"));
            return;
        }
        String playerIdentifier = args[0];
        String board = args[1];
        if(!plugin.getCache().boardExists(board) && !"*".equals(board)) {
            sender.sendMessage(message("&cThe board '"+board+"' does not exist."));
            return;
        }
        UUID playerUuid = parseUuid(playerIdentifier);
        List<String> boards = Collections.singletonList(board);
        if("*".equals(board)) {
            boards = plugin.getCache().getBoards();
        }
        List<String> finalBoards = boards;
        plugin.getScheduler().runTaskAsynchronously(() -> {
            for(String b : finalBoards) {
                boolean success;
                if(playerUuid != null) {
                    success = plugin.getCache().removePlayer(b, playerUuid);
                } else {
                    success = plugin.getCache().removePlayer(b, playerIdentifier);
                }
                if(success) {
                    sender.sendMessage(message("&aRemoved "+playerIdentifier+" from "+b+"!"));
                } else {
                    sender.sendMessage(message("&cUnable to remove "+playerIdentifier+" from "+b+". &7Check the console for more info."));
                }
            }
            if("*".equals(board)) {
                sender.sendMessage(message("&aFinished removing "+playerIdentifier+" from all boards!"));
            }
        });
    }

    private UUID parseUuid(String input) {
        if(input == null) return null;
        String trimmed = input.trim();
        try {
            if(trimmed.length() == 36 && trimmed.indexOf('-') >= 0) {
                return UUID.fromString(trimmed);
            }
            if(trimmed.length() == 32) {
                String dashed = trimmed.substring(0, 8) + "-" + trimmed.substring(8, 12) + "-"
                        + trimmed.substring(12, 16) + "-" + trimmed.substring(16, 20) + "-"
                        + trimmed.substring(20, 32);
                return UUID.fromString(dashed);
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }
}
