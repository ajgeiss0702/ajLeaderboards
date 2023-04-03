package us.ajg0702.leaderboards.boards.keys;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Objects;

public class PlayerBoardType {
    private final OfflinePlayer player;
    private final String board;
    private final TimedType type;

    public PlayerBoardType(OfflinePlayer player, String board, TimedType type) {
        this.player = player;
        this.board = board;
        this.type = type;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public String getBoard() {
        return board;
    }

    public TimedType getType() {
        return type;
    }

    public BoardType getBoardType() {
        return new BoardType(board, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerBoardType)) return false;
        PlayerBoardType that = (PlayerBoardType) o;
        return getPlayer().equals(that.getPlayer()) && getBoard().equals(that.getBoard()) && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPlayer(), getBoard(), getType());
    }

    @Override
    public String toString() {
        return "PlayerBoardType{" +
                "player=" + player +
                ", board='" + board + '\'' +
                ", type=" + type +
                '}';
    }
}
