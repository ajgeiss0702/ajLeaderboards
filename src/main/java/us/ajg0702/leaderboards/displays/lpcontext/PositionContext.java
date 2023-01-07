package us.ajg0702.leaderboards.displays.lpcontext;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.keys.BoardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PositionContext implements ContextCalculator<Player> {
    private final LeaderboardPlugin plugin;

    List<BoardType> contextBoardTypes = new ArrayList<>();

    public PositionContext(LeaderboardPlugin leaderboardPlugin) {
        plugin = leaderboardPlugin;

        calculatePotentialContexts();
    }

    @Override
    public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        for (BoardType contextBoardType : contextBoardTypes) {
            consumer.accept(
                    "ajlb_pos_"+contextBoardType.getBoard()+"_"+contextBoardType.getType().lowerName(),
                    plugin.getTopManager().getStatEntry(
                            target, contextBoardType.getBoard(), contextBoardType.getType()
                    ).getPosition()+""
            );
        }

    }

    private ContextSet potentialContexts;
    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        return potentialContexts;
    }

    public void calculatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        for (BoardType contextBoardType : contextBoardTypes) {
            for (int i = 1; i <= 10; i++) {
                builder.add("ajlb_pos_"+contextBoardType.getBoard()+"_"+contextBoardType.getType().lowerName(), i+"");
            }
        }

        potentialContexts = builder.build();
    }

    public void reloadConfig() {
        contextBoardTypes.clear();

        if(plugin.getAConfig().getStringList("only-register-lpc-for").isEmpty()) {
            for (String board : plugin.getTopManager().getBoards()) {
                contextBoardTypes.addAll(allBoardTypes(board));
            }
        } else {
            List<String> btRaws = plugin.getAConfig().getStringList("only-register-lpc-for");
            for (String btRaw : btRaws) {
                String board = btRaw;
                if(board.contains(":")) {
                    int colonPosition = board.lastIndexOf(":");
                    board = board.substring(0, colonPosition);
                    String typeRaw = btRaw.substring(colonPosition + 1).toUpperCase(Locale.ROOT); // +1 to not include the colon

                    try {
                        TimedType type = TimedType.valueOf(typeRaw);
                        contextBoardTypes.add(new BoardType(board, type));
                    } catch(IllegalArgumentException e) {
                        // if the part after the colon is not a timed type, assume its part of the placeholder
                        Debug.info("[Context filter] Assuming" + btRaw + " is a board name! (the stuff after colon is not a timed type: '" + typeRaw + "'");
                        contextBoardTypes.addAll(allBoardTypes(btRaw));
                    }
                } else {
                    // if it doesn't have a :, treat it as a board and add all types from that board
                    contextBoardTypes.addAll(allBoardTypes(board));
                }
            }
        }

        calculatePotentialContexts();
    }

    private List<BoardType> allBoardTypes(String board) {
        List<BoardType> r = new ArrayList<>();
        for (TimedType type : TimedType.values()) {
            r.add(new BoardType(board, type));
        }
        return r;
    }
}
