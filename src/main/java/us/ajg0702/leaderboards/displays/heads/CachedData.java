package us.ajg0702.leaderboards.displays.heads;

public class CachedData<T> {
    private final T data;
    private final long time;
    public CachedData(T data) {
        this(data, System.currentTimeMillis());
    }
    public CachedData(T data, long time) {
        this.data = data;
        this.time = time;
    }

    public T getData() {
        return data;
    }

    public long getTime() {
        return time;
    }

    public long getTimeSince() {
        return System.currentTimeMillis() - getTime();
    }
}
