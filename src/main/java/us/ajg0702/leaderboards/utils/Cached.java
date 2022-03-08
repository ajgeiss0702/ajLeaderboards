package us.ajg0702.leaderboards.utils;

import java.util.concurrent.Future;

public class Cached<T> {
    private final long lastGet;
    private final T thing;

    public Cached(T thing) {
        this(System.currentTimeMillis(), thing);
    }
    public Cached(long lastGet, T thing) {
        this.lastGet = lastGet;
        this.thing = thing;
    }

    public long getLastGet() {
        return lastGet;
    }

    public T getThing() {
        return thing;
    }

    public static Cached<Future<Long>> none() {
        return new Cached<>(-1, null);
    }
}
