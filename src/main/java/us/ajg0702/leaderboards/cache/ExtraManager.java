package us.ajg0702.leaderboards.cache;

import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;

@SuppressWarnings("FieldCanBeLocal")
public class ExtraManager {
    private final LeaderboardPlugin plugin;
    private final Cache cache;
    private final CacheMethod method;

    public ExtraManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        this.cache = plugin.getCache();

        if(cache == null) {
            throw new IllegalStateException("Cache not found. Has it been loaded?");
        }
        this.method = cache.getMethod();
        this.method.createExtraTable();
    }

    public String getExtra(UUID id, String placeholder) {
        return method.getExtra(id, placeholder);
    }

    public List<String> getExtras() {
        List<String> extras = new ArrayList<>();
        for (String extra : plugin.getAConfig().getStringList("extras")) {
            extra = extra.replaceAll(Matcher.quoteReplacement("%"), "");
            extras.add(extra);
        }
        return extras;
    }

    public void setExtra(UUID id, String placeholder, String value) {
        if(plugin.isShuttingDown()) return;
        method.upsertExtra(id, placeholder, value);
    }

}
