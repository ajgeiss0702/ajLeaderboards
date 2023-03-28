package us.ajg0702.leaderboards.boards.keys;

import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Objects;

public class BoardType {
    private final String board;
    private final TimedType type;

    public BoardType(String board, TimedType type) {
        this.board = board;
        this.type = type;
    }

    public String getBoard() {
        return board;
    }

    public TimedType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardType)) return false;
        BoardType boardType = (BoardType) o;
        return getBoard().equals(boardType.getBoard()) && getType() == boardType.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBoard(), getType());
    }

    @Override
    public String toString() {
        return board + " " + type.lowerName();
    }
}
