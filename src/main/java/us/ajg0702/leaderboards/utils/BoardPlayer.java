package us.ajg0702.leaderboards.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Objects;
import java.util.UUID;

public class BoardPlayer {

    private final String board;
    private final UUID playerId;

    public BoardPlayer(String board, OfflinePlayer player) {
        this.board = board;
        this.playerId = player.getUniqueId();
    }

    public String getBoard() {
        return board;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(playerId);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardPlayer)) return false;
        BoardPlayer that = (BoardPlayer) o;
        return getBoard().equals(that.getBoard()) && getPlayerId().equals(that.getPlayerId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBoard(), getPlayerId());
    }
}
