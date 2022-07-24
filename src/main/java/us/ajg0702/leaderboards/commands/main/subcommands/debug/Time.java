package us.ajg0702.leaderboards.commands.main.subcommands.debug;

import net.kyori.adventure.text.Component;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class Time extends SubCommand {
    public Time() {
        super("time", Collections.emptyList(), null, "Shows the current time");
        setShowInTabComplete(false);
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        sender.sendMessage(Component.text(LocalDateTime.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
    }
}
