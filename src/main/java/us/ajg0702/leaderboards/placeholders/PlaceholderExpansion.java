package us.ajg0702.leaderboards.placeholders;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.placeholders.placeholders.Reset;
import us.ajg0702.leaderboards.placeholders.placeholders.debug.Fetching;
import us.ajg0702.leaderboards.placeholders.placeholders.debug.Rolling;
import us.ajg0702.leaderboards.placeholders.placeholders.lb.*;
import us.ajg0702.leaderboards.placeholders.placeholders.player.*;
import us.ajg0702.leaderboards.placeholders.placeholders.relative.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final List<Placeholder> placeholders = new ArrayList<>();

    private final LeaderboardPlugin plugin;

    @SuppressWarnings("deprecated")
    public PlaceholderExpansion(LeaderboardPlugin plugin) {

        this.plugin = plugin;

        placeholders.add(new Extra(plugin));
        placeholders.add(new Name(plugin));
        placeholders.add(new Prefix(plugin));
        placeholders.add(new Suffix(plugin));
        placeholders.add(new Color(plugin));
        placeholders.add(new RawValue(plugin));
        placeholders.add(new ValueFormatted(plugin));
        placeholders.add(new Value(plugin));
        placeholders.add(new Time(plugin));
        placeholders.add(new DisplayName(plugin));

        placeholders.add(new PlayerPosition(plugin));
        placeholders.add(new PlayerValueFormatted(plugin));
        placeholders.add(new PlayerValueRaw(plugin));
        placeholders.add(new PlayerValueTime(plugin));
        placeholders.add(new PlayerValue(plugin));
        placeholders.add(new PlayerTime(plugin));
        placeholders.add(new PlayerExtra(plugin));


        placeholders.add(new RelColor(plugin));
        placeholders.add(new RelDisplayName(plugin));
        placeholders.add(new RelExtra(plugin));
        placeholders.add(new RelName(plugin));
        placeholders.add(new RelPosition(plugin));
        placeholders.add(new RelPrefix(plugin));
        placeholders.add(new RelRawValue(plugin));
        placeholders.add(new RelSuffix(plugin));
        placeholders.add(new RelTime(plugin));
        placeholders.add(new RelValue(plugin));
        placeholders.add(new RelValueFormatted(plugin));

        placeholders.add(new Reset(plugin));

        placeholders.add(new Fetching(plugin));
        placeholders.add(new Rolling(plugin));
    }

    Map<String, CachedPlaceholder> placeholderCache = new HashMap<>();

    @Override
    public String onRequest(OfflinePlayer p, @NotNull String params) {
        CachedPlaceholder cachedPlaceholder = placeholderCache.computeIfAbsent(params, s -> {
            for(Placeholder placeholder : placeholders) {
                Matcher matcher = placeholder.getPattern().matcher(params);
                if(!matcher.matches()) continue;
                return new CachedPlaceholder(matcher, placeholder);
            }
            return null;
        });
        if(cachedPlaceholder == null) return null;

        return cachedPlaceholder.getPlaceholder().parse(cachedPlaceholder.getMatcher(), p);
    }



    @Override
    public @NotNull String getIdentifier() {
        return "ajlb";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ajgeiss0702";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }
}
