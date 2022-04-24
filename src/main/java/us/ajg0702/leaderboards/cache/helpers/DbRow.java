package us.ajg0702.leaderboards.cache.helpers;

import com.google.gson.JsonObject;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.utils.EasyJsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DbRow {

    private final UUID id;
    private final double value;

    private final Map<TimedType, Double> deltas;
    private final Map<TimedType, Double> lastTotals;
    private final Map<TimedType, Long> timestamps;

    private final String namecache;
    private final String prefixcache;
    private final String suffixcache;
    private final String displaynamecache;



    private static final Map<String, Integer> positionCache = new HashMap<>();
    public DbRow(ResultSet resultSet) throws SQLException {
        this(
                UUID.fromString(resultSet.getString(getIndex(resultSet, "id"))),
                resultSet.getDouble(getIndex(resultSet, "value")),
                getTypeMaps(resultSet),
                resultSet.getString(getIndex(resultSet, "namecache")),
                resultSet.getString(getIndex(resultSet, "prefixcache")),
                resultSet.getString(getIndex(resultSet, "suffixcache")),
                resultSet.getString(getIndex(resultSet, "displaynamecache"))
        );
    }

    private DbRow(UUID id, double value, List<Object> typeMaps, String namecache, String prefixcache, String suffixcache, String displaynamecache) {
        //noinspection unchecked
        this(
                id,
                value,
                (Map<TimedType, Double>) typeMaps.get(0),
                (Map<TimedType, Double>) typeMaps.get(1),
                (Map<TimedType, Long>) typeMaps.get(2),
                namecache,
                prefixcache,
                suffixcache,
                displaynamecache
        );
    }

    public DbRow(UUID id, double value, Map<TimedType, Double> deltas, Map<TimedType, Double> lastTotals, Map<TimedType, Long> timestamps, String namecache, String prefixcache, String suffixcache, String displaynamecache) {
        this.id = id;
        this.value = value;
        this.deltas = deltas;
        this.lastTotals = lastTotals;
        this.timestamps = timestamps;
        this.namecache = namecache;
        this.prefixcache = prefixcache;
        this.suffixcache = suffixcache;
        this.displaynamecache = displaynamecache;
    }

    private static List<Object> getTypeMaps(ResultSet resultSet) throws SQLException {
        final Map<TimedType, Double> deltas = new HashMap<>();
        final Map<TimedType, Double> lastTotals = new HashMap<>();
        final Map<TimedType, Long> timestamps = new HashMap<>();

        for(TimedType type : TimedType.values()) {
            if(type == TimedType.ALLTIME) continue;

            deltas.put(type, resultSet.getDouble(getIndex(resultSet, type.lowerName()+"_delta")));
            lastTotals.put(type, resultSet.getDouble(getIndex(resultSet, type.lowerName()+"_lasttotal")));
            timestamps.put(type, resultSet.getLong(getIndex(resultSet, type.lowerName()+"_timestamp")));
        }
        return Arrays.asList(deltas, lastTotals, timestamps);
    }

    private static int getIndex(ResultSet rs, String name) throws SQLException {
        if(!positionCache.containsKey(name)) {
            int position = rs.findColumn(name);
            positionCache.put(name, position);
            return position;
        }
        return positionCache.get(name);
    }

    public UUID getId() {
        return id;
    }
    public double getValue() {
        return value;
    }
    public Map<TimedType, Double> getDeltas() {
        return deltas;
    }
    public Map<TimedType, Double> getLastTotals() {
        return lastTotals;
    }
    public Map<TimedType, Long> getTimestamps() {
        return timestamps;
    }
    public String getNamecache() {
        return namecache;
    }
    public String getPrefixcache() {
        return prefixcache;
    }
    public String getSuffixcache() {
        return suffixcache;
    }
    public String getDisplaynamecache() {
        return displaynamecache;
    }


    public JsonObject toJsonObject() {
        EasyJsonObject out = new EasyJsonObject()
                .add("id", getId().toString())
                .add("value", getValue());
        for(TimedType type : TimedType.values()) {
            String lowerName = type.lowerName();
            out.add(lowerName+"_delta", getDeltas().get(type));
            out.add(lowerName+"_lasttotal", getLastTotals().get(type));
            out.add(lowerName+"_timestamp", getTimestamps().get(type));
        }
        out.add("namecache", getNamecache());
        out.add("prefixcache", getPrefixcache());
        out.add("suffixcache", getSuffixcache());
        out.add("displaynamecache", getDisplaynamecache());
        return out.getHandle();
    }

    public static DbRow fromJsonObject(JsonObject object) {
        final Map<TimedType, Double> deltas = new HashMap<>();
        final Map<TimedType, Double> lastTotals = new HashMap<>();
        final Map<TimedType, Long> timestamps = new HashMap<>();

        for(TimedType type : TimedType.values()) {
            if(type == TimedType.ALLTIME) continue;

            deltas.put(type, object.get(type.lowerName()+"_delta").getAsDouble());
            lastTotals.put(type, object.get(type.lowerName()+"_lasttotal").getAsDouble());
            timestamps.put(type, object.get(type.lowerName()+"_timestamp").getAsLong());
        }

        return new DbRow(
                UUID.fromString(object.get("id").getAsString()),
                object.get("value").getAsDouble(),
                deltas, lastTotals, timestamps,
                object.get("namecache").getAsString(),
                object.get("prefixcache").getAsString(),
                object.get("suffixcache").getAsString(),
                object.get("displaynamecache").getAsString()
        );
    }
}
