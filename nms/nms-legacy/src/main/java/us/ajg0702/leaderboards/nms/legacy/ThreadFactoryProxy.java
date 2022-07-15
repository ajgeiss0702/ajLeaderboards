package us.ajg0702.leaderboards.nms.legacy;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoryProxy {
    public static ThreadFactory getDefaultThreadFactory(String name) {
        return new DefaultThreadFactory(name);
    }
}
