package us.ajg0702.leaderboards.boards.keys;

import us.ajg0702.leaderboards.boards.TimedType;

import java.util.Objects;

public class PositionBoardType {
    private final int position;
    private final String board;
    private final TimedType type;

    public PositionBoardType(int position, String board, TimedType type) {
        this.position = position;
        this.board = board;
        this.type = type;
    }

    public int getPosition() {
        return position;
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
        if (!(o instanceof PositionBoardType)) return false;
        PositionBoardType that = (PositionBoardType) o;
        return getPosition() == that.getPosition() && getBoard().equals(that.getBoard()) && getType() == that.getType();
    }

    @Override
    public String toString() {
        return position + ":" + board + ":" + type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPosition(), getBoard(), getType());
    }
}
