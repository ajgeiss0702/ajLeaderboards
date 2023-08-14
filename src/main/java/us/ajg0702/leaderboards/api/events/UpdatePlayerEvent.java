package us.ajg0702.leaderboards.api.events;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.ajg0702.leaderboards.utils.BoardPlayer;

import java.util.UUID;

public class UpdatePlayerEvent extends Event implements Cancellable {

    private final BoardPlayer boardPlayer;
    private boolean cancelled = false;

    public UpdatePlayerEvent(BoardPlayer boardPlayer) {
        super(true);
        this.boardPlayer = boardPlayer;
    }

    public BoardPlayer getBoardPlayer() {
        return boardPlayer;
    }

    public String getBoard() {
        return getBoardPlayer().getBoard();
    }

    public OfflinePlayer getPlayer() {
        return getBoardPlayer().getPlayer();
    }

    public UUID getPlayerId() {
        return getBoardPlayer().getPlayerId();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    private static final HandlerList HANDLERS = new HandlerList();

    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
