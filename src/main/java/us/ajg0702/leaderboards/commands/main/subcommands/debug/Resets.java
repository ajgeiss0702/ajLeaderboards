package us.ajg0702.leaderboards.commands.main.subcommands.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Resets extends SubCommand {
    private final LeaderboardPlugin plugin;
    public Resets(LeaderboardPlugin plugin) {
        super("resets", Collections.emptyList(), null, "Show reset times");
        setShowInTabComplete(false);
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        if(args.length == 1) return plugin.getTopManager().getBoards();
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if(args.length == 0) {
            sender.sendMessage(Component.text("Need board name!").color(NamedTextColor.RED));
            return;
        }

        String board = args[0];

        if(!plugin.getTopManager().boardExists(board)) {
            sender.sendMessage(Component.text("Invalid board").color(NamedTextColor.RED));
            return;
        }

        for (TimedType type : TimedType.values()) {
            if(type == TimedType.ALLTIME) continue;

            sender.sendMessage(Component.text(type.toString()).color(NamedTextColor.YELLOW));
            sender.sendMessage(
                    Component.text("  Next: ").color(NamedTextColor.GOLD).append(
                            Component.text(formatTime(type.getNextReset())).color(NamedTextColor.YELLOW)
                    )
            );
            sender.sendMessage(
                    Component.text("  Last: ").color(NamedTextColor.GOLD).append(
                            Component.text(
                                    formatTime(
                                            LocalDateTime.ofEpochSecond(
                                                    plugin.getTopManager().getLastReset(board, type),
                                                    0, ZoneOffset.UTC
                                            )
                                    )
                            ).color(NamedTextColor.YELLOW)
                    )
            );
            sender.sendMessage(
                    Component.text("  Est. last: ").color(NamedTextColor.GOLD).append(
                            Component.text(
                                    formatTime(type.getEstimatedLastReset())
                            ).color(NamedTextColor.YELLOW)
                    )
            );

        }
    }
    private String formatTime(LocalDateTime time) {

        List<String> parts = new ArrayList<>(
                Arrays.asList(
                        time.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME).split(" ")
                )
        );
        parts.remove(parts.size()-1);
        return String.join(" ", parts);
    }
}
