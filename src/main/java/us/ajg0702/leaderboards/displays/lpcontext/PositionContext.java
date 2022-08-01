package us.ajg0702.leaderboards.displays.lpcontext;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;

public class PositionContext implements ContextCalculator<Player> {
    private final LeaderboardPlugin plugin;

    public PositionContext(LeaderboardPlugin leaderboardPlugin) {
        plugin = leaderboardPlugin;

        calculatePotentialContexts();
    }

    @Override
    public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        for (String board : plugin.getTopManager().getBoards()) {
            for (TimedType type : TimedType.values()) {
                consumer.accept("ajlb_pos_"+board+"_"+type.lowerName(), plugin.getTopManager().getStatEntry(target, board, type).getPosition()+"");
            }
        }
    }

    private ContextSet potentialContexts;
    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        return potentialContexts;
    }

    public void calculatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        for (String board : plugin.getTopManager().getBoards()) {
            for (TimedType type : TimedType.values()) {
                for (int i = 1; i <= 10; i++) {
                    builder.add("ajlb_pos_"+board+"_"+type.lowerName(), i+"");
                }
            }
        }
        potentialContexts = builder.build();
    }
}
