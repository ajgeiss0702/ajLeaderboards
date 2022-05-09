package us.ajg0702.leaderboards.boards;

import java.util.Objects;
import java.util.UUID;

public class ExtraKey {
    private final UUID id;
    private final String placeholder;

    public ExtraKey(UUID id, String placeholder) {
        this.id = id;
        this.placeholder = placeholder;
    }

    public UUID getId() {
        return id;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtraKey)) return false;
        ExtraKey extraKey = (ExtraKey) o;
        return getId().equals(extraKey.getId()) && Objects.equals(getPlaceholder(), extraKey.getPlaceholder());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getPlaceholder());
    }
}
