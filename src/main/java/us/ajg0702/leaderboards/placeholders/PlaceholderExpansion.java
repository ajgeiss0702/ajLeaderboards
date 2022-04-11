package us.ajg0702.leaderboards.placeholders;

import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.placeholders.placeholders.Fetching;
import us.ajg0702.leaderboards.placeholders.placeholders.lb.*;
import us.ajg0702.leaderboards.placeholders.placeholders.player.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final List<Placeholder> placeholders = new ArrayList<>();

    private final LeaderboardPlugin plugin;

    public PlaceholderExpansion(LeaderboardPlugin plugin) {

        this.plugin = plugin;

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

        placeholders.add(new Fetching(plugin));
    }

    @Override
    public String onRequest(OfflinePlayer p, String params) {
        for(Placeholder placeholder : placeholders) {
            Matcher matcher = placeholder.getPattern().matcher(params);
            if(!matcher.matches()) continue;
            return placeholder.parse(matcher, p);
        }
        return null;
    }



    @Override
    public String getIdentifier() {
        return "ajlb";
    }

    @Override
    public String getAuthor() {
        return "ajgeiss0702";
    }

    @Override
    public String getVersion() {
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
