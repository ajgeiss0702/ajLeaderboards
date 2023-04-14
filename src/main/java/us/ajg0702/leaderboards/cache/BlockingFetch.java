package us.ajg0702.leaderboards.cache;

import org.bukkit.Bukkit;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.util.Locale;

public enum BlockingFetch {
    TRUE, FALSE, AUTO;

    public static boolean shouldBlock(LeaderboardPlugin plugin) {
        String raw = plugin.getAConfig().getString("blocking-fetch");

        BlockingFetch blockingFetch;
        try {
            blockingFetch = valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid option for blocking-fetch. Defaulting to auto.");
            blockingFetch = AUTO;
        }

        if(blockingFetch.isBoolean()) return blockingFetch.getBoolean();

        // auto
        return !Bukkit.isPrimaryThread();
    }

    private boolean isBoolean() {
        return this.equals(TRUE) || this.equals(FALSE);
    }

    private boolean getBoolean() {
        if(this.equals(TRUE)) return true;
        if(this.equals(FALSE)) return false;
        throw new IllegalStateException("Not a boolean!");
    }
}
