package us.ajg0702.leaderboards.boards.keys;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Objects;
import java.util.UUID;

public class PlayerBoardType {
    private final UUID playerId;
    private final String board;
    private final TimedType type;

    public PlayerBoardType(OfflinePlayer player, String board, TimedType type) {
        this.playerId = player.getUniqueId();
        this.board = board;
        this.type = type;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(playerId);
    }

    public UUID getPlayerId() {
        return playerId;
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
        return getPlayerId().equals(that.getPlayerId()) && getBoard().equals(that.getBoard()) && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPlayerId(), getBoard(), getType());
    }

    @Override
    public String toString() {
        return "PlayerBoardType{" +
                "playerId=" + playerId +
                ", board='" + board + '\'' +
                ", type=" + type +
                '}';
    }
}
