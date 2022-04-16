package us.ajg0702.leaderboards.nms;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoryProxy {
    public static ThreadFactory getDefaultThreadFactory(String name) {
        return new DefaultThreadFactory(name);
    }
}
