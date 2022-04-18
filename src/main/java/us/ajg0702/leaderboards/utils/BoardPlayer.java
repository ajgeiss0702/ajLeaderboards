package us.ajg0702.leaderboards.utils;

import org.bukkit.OfflinePlayer;

import java.util.Objects;

public class BoardPlayer {

    private final String board;
    private final OfflinePlayer player;

    public BoardPlayer(String board, OfflinePlayer player) {
        this.board = board;
        this.player = player;
    }

    public String getBoard() {
        return board;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardPlayer)) return false;
        BoardPlayer that = (BoardPlayer) o;
        return getBoard().equals(that.getBoard()) && getPlayer().equals(that.getPlayer());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBoard(), getPlayer());
    }
}
