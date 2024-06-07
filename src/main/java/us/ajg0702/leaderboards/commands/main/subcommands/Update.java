package us.ajg0702.leaderboards.commands.main.subcommands;

import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.utils.common.UpdateManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Update extends SubCommand {
    private final LeaderboardPlugin plugin;
    public Update(LeaderboardPlugin plugin) {
        super("update", Collections.singletonList("updateplugin"), "ajLeaderboards.use", "Download an update for ajLeaderboards (if it is available)");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings, String s) {

        UpdateManager updater = plugin.getUpdateManager();

        if(updater == null) {
            commandSender.sendMessage(
                    plugin.getMessages().getComponent("updater.disabled")
            );
            return;
        }

        AtomicBoolean done = new AtomicBoolean(false);
        plugin.getScheduler().runTaskAsynchronously(() -> {
            try {
                UpdateManager.DownloadCompleteStatus result = updater.downloadUpdate();

                done.set(true);

                switch(result) {
                    case SUCCESS:
                        commandSender.sendMessage(plugin.getMessages().getComponent("updater.success"));
                        break;
                    case WARNING_COULD_NOT_DELETE_OLD_JAR:
                        commandSender.sendMessage(plugin.getMessages().getComponent("updater.warnings.could-not-delete-old-jar"));
                        break;
                    case ERROR_NO_UPDATE_AVAILABLE:
                        commandSender.sendMessage(plugin.getMessages().getComponent("updater.errors.no-update-available"));
                        break;
                    case ERROR_WHILE_CHECKING:
                        commandSender.sendMessage(plugin.getMessages().getComponent("updater.errors.while-checking"));
                        break;
                    case ERROR_ALREADY_DOWNLOADED:
                        commandSender.sendMessage(plugin.getMessages().getComponent("updater.errors.already-downloaded"));
                        break;
                    case ERROR_WHILE_DOWNLOADING:
                        commandSender.sendMessage(plugin.getMessages().getComponent("updater.errors.while-downloading"));
                        break;
                    default:
                        commandSender.sendMessage(plugin.getMessages().getComponent("updater.errors.unknown", "ERROR:" + result));
                        break;
                }
            } catch(Exception e) {
                commandSender.sendMessage(plugin.getMessages().getComponent("updater.errors.uncaught"));
            }
        });
        plugin.getScheduler().runTaskLaterAsynchronously(() -> {
            if(done.get()) return;

            commandSender.sendMessage(
                    plugin.getMessages().getComponent("updater.slow-feedback")
            );

        }, 20);
    }
}
