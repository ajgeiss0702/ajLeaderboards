package us.ajg0702.leaderboards.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.ajg0702.leaderboards.boards.TimedType;

public class PreTimedTypeResetEvent extends Event {

    private final String board;
    private final TimedType type;


    public PreTimedTypeResetEvent(String board, TimedType type) {
        this.board = board;
        this.type = type;
    }

    public String getBoard() {
        return board;
    }

    public TimedType getType() {
        return type;
    }

    private static final HandlerList HANDLERS = new HandlerList();

    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
