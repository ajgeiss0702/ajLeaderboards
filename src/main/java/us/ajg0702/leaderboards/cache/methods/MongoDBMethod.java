package us.ajg0702.leaderboards.cache.methods;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.keys.BoardType;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.leaderboards.cache.helpers.DbRow;
import us.ajg0702.leaderboards.utils.Partition;
import us.ajg0702.utils.common.ConfigFile;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBMethod implements CacheMethod {
    private final ConfigFile storageConfig;
    private final String tablePrefix;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private LeaderboardPlugin plugin;

    public MongoDBMethod(ConfigFile storageConfig) {
        this.storageConfig = storageConfig;
        this.tablePrefix = storageConfig.getString("table_prefix");
    }

    @Override
    public void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance) {
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().conventions(Arrays.asList(Conventions.ANNOTATION_CONVENTION, Conventions.CLASS_AND_PROPERTY_CONVENTION,
                        Conventions.OBJECT_ID_GENERATORS, Conventions.SET_PRIVATE_FIELDS_CONVENTION)).build()));
        ConnectionString connString = new ConnectionString(storageConfig.getString("mongoConnectionString"));
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .retryWrites(true)
                .codecRegistry(codecRegistry)
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
        this.mongoClient = MongoClients.create(settings);
        this.mongoDatabase = this.mongoClient.getDatabase(this.storageConfig.getString("mongoDatabase"));
        this.plugin = plugin;
    }

    @Override
    public void shutdown() {
        mongoClient.close();
    }

    @Override
    public String getName() {
        return "mongodb";
    }

    @Override
    public boolean requiresClose() {
        return false;
    }

    @Override
    public StatEntry getStatEntry(int position, String board, TimedType type) throws SQLException {
        String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
        StatEntry result = mongoDatabase.getCollection(tablePrefix + board, StatEntry.class).find()
                .sort(isReverse(board) ? Sorts.descending(sortBy) : Sorts.ascending(sortBy))
                .skip(position - 1).first();
        return result == null ? StatEntry.boardNotFound(this.plugin, position, board, type) : result;
    }

    @Nullable
    @Override
    public StatEntry getStatEntry(OfflinePlayer player, String board, TimedType type) {
        String sortBy = type == TimedType.ALLTIME ? "value" : type.lowerName() + "_delta";
        StatEntry result = mongoDatabase.getCollection(tablePrefix + board, StatEntry.class)
                .find()
                .filter(Filters.eq("playerID", player.getUniqueId()))
                .sort(isReverse(board) ? Sorts.descending(sortBy) : Sorts.ascending(sortBy))
                .first();
        return result == null ? StatEntry.boardNotFound(this.plugin, -1, board, type) : result;
    }

    @Override
    public int getBoardSize(String board) {
        return (int) mongoDatabase.getCollection(tablePrefix + board, StatEntry.class).countDocuments();
    }

    @Override
    public double getLastTotal(String board, OfflinePlayer player, TimedType type) {
        Document result = mongoDatabase.getCollection(tablePrefix + board)
                .find()
                .filter(Filters.eq("playerID", player.getUniqueId()))
                .first();
        if (result == null) {
            return 0;
        }
        return result.get(type.lowerName() + "_delta", Double.class);
    }

    @Override
    public long getLastReset(String board, TimedType type) {
        Document result = mongoDatabase.getCollection(tablePrefix + board)
                .find()
                .first();
        if (result == null) {
            return 0;
        }
        return result.get(type.lowerName() + "_timestamp", Long.class);
    }

    @Override
    public void resetBoard(String board, TimedType type, long newTime) {
        Map<String, Double> uuids = new HashMap<>();
        for (Document document : mongoDatabase.getCollection(tablePrefix + board).find()) {
            uuids.put(document.getString("playerID"), document.getDouble(type.lowerName() + "_delta"));
        }
        Partition<String> partition = Partition.ofSize(new ArrayList<>(uuids.keySet()), Math.max(uuids.size(), 1));
        Debug.info("Partition length: " + partition.size() + " uuids size: " + uuids.size() + " partition chunk size: " + partition.getChunkSize());
        for (List<String> uuidPartition : partition) {
            try {
                for (String idRaw : uuidPartition) {
                    if (this.plugin.isShuttingDown()) {
                        return;
                    }
                    mongoDatabase.getCollection(tablePrefix + board)
                            .updateOne(Filters.eq("playerID", idRaw),
                                    Updates.combine(Updates.set(type.lowerName() + "_lasttotal", uuids.get(idRaw))
                                            , Updates.set(type.lowerName() + "_delta", 0)
                                            , Updates.set(type.lowerName() + "_timestamp", newTime)));
                }
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "An error occurred while resetting " + type + " of " + board + ":", e);
            }
        }
    }

    @Override
    public void insertRows(String board, List<DbRow> rows) {
        for (DbRow row : rows) {
            Document document = new Document();
            document.put("playerID", row.getId());
            document.put("value", row.getValue());
            document.put("namecache", row.getNamecache());
            document.put("prefixcache", row.getPrefixcache());
            document.put("suffixcache", row.getSuffixcache());
            document.put("displaynamecache", row.getDisplaynamecache());
            for (TimedType type : TimedType.values()) {
                if (type == TimedType.ALLTIME) continue;
                document.put(type.lowerName() + "_delta", row.getDeltas().get(type));
                document.put(type.lowerName() + "_lasttotal", row.getLastTotals().get(type));
                document.put(type.lowerName() + "_timestamp", row.getTimestamps().get(type));
            }
            mongoDatabase.getCollection(tablePrefix + board).insertOne(document);
        }
    }

    @Override
    public List<DbRow> getRows(String board) {
        List<DbRow> rows = new ArrayList<>();
        for (Document document : mongoDatabase.getCollection(tablePrefix + board).find()) {
            Map<TimedType, Double> deltas = new HashMap<>();
            Map<TimedType, Double> lastTotals = new HashMap<>();
            Map<TimedType, Long> timestamps = new HashMap<>();
            for (TimedType type : TimedType.values()) {
                if (type == TimedType.ALLTIME) continue;
                deltas.put(type, document.getDouble(type.lowerName() + "_delta"));
                lastTotals.put(type, document.getDouble(type.lowerName() + "_lasttotal"));
                timestamps.put(type, document.getLong(type.lowerName() + "_timestamp"));
            }
            rows.add(new DbRow(document.get("playerID", UUID.class), document.getDouble("value"), deltas, lastTotals, timestamps, document.getString("namecache"), document.getString("prefixcache"), document.getString("suffixcache"), document.getString("displaynamecache")));
        }
        return rows;
    }

    @Override
    public boolean createBoard(String name) {
        mongoDatabase.createCollection(tablePrefix + name);
        return true;
    }

    @Override
    public boolean removePlayer(String board, String playerName) {
        return mongoDatabase.getCollection(tablePrefix + board).deleteOne(Filters.eq("namecache", playerName)).getDeletedCount() > 0;
    }

    @Override
    public void upsertPlayer(String board, OfflinePlayer player, double output, String prefix, String suffix, String displayName) {
        Map<TimedType, Double> lastTotals = new HashMap<>();
        for (TimedType type : TimedType.values()) {
            if (type == TimedType.ALLTIME) continue;
            lastTotals.put(type, getLastTotal(board, player, type));
        }
        Document result = mongoDatabase.getCollection(tablePrefix + board)
                .find()
                .filter(Filters.eq("playerID", player.getUniqueId()))
                .first();
        if (result == null) {
            Document document = new Document();
            document.put("playerID", player.getUniqueId());
            document.put("value", output);
            document.put("namecache", player.getName());
            document.put("prefixcache", prefix);
            document.put("suffixcache", suffix);
            document.put("displaynamecache", displayName);
            for (TimedType type : TimedType.values()) {
                if (type == TimedType.ALLTIME) continue;
                document.put(type.lowerName() + "_delta", output - lastTotals.get(type));
                document.put(type.lowerName() + "_lasttotal", output);
                document.put(type.lowerName() + "_timestamp", System.currentTimeMillis());
            }
            mongoDatabase.getCollection(tablePrefix + board).insertOne(document);
        } else {
            Map<TimedType, Double> timedTypeValues = new HashMap<>();
            timedTypeValues.put(TimedType.ALLTIME, output);
            for (TimedType type : TimedType.values()) {
                if (type == TimedType.ALLTIME) continue;
                double timedOut = output - lastTotals.get(type);
                timedTypeValues.put(type, timedOut);
            }
            for (Map.Entry<TimedType, Double> timedTypeDoubleEntry : timedTypeValues.entrySet()) {
                TimedType type = timedTypeDoubleEntry.getKey();
                double timedOut = timedTypeDoubleEntry.getValue();

                StatEntry statEntry = this.plugin.getTopManager().getCachedStatEntry(player, board, type, false);
                if (statEntry != null && player.getUniqueId().equals(statEntry.getPlayerID())) {
                    statEntry.changeScore(timedOut, prefix, suffix);
                }

                Integer position = this.plugin.getTopManager()
                        .positionPlayerCache.getOrDefault(player.getUniqueId(), new HashMap<>())
                        .get(new BoardType(board, type));
                if (position != null) {
                    StatEntry stat = this.plugin.getTopManager().getCachedStat(position, board, type);
                    if (stat != null && player.getUniqueId().equals(stat.getPlayerID())) {
                        stat.changeScore(timedOut, prefix, suffix);
                    }
                }
            }

            Document document = new Document();
            document.put("playerID", player.getUniqueId());
            document.put("value", output);
            document.put("namecache", player.getName());
            document.put("prefixcache", prefix);
            document.put("suffixcache", suffix);
            document.put("displaynamecache", displayName);
            for (TimedType type : TimedType.values()) {
                if (type == TimedType.ALLTIME) continue;
                document.put(type.lowerName() + "_delta", output - lastTotals.get(type));
                document.put(type.lowerName() + "_lasttotal", output);
                document.put(type.lowerName() + "_timestamp", System.currentTimeMillis());
            }
            mongoDatabase.getCollection(tablePrefix + board).replaceOne(Filters.eq("playerID", player.getUniqueId()), document);
        }
    }

    @Override
    public boolean removeBoard(String board) {
        mongoDatabase.getCollection(tablePrefix + board).drop();
        this.plugin.getTopManager().fetchBoards();
        this.plugin.getContextLoader().calculatePotentialContexts();
        if (this.plugin.getTopManager().boardExists(board)) {
            this.plugin.getLogger().warning("Attempted to remove a board, but it didnt get removed!");
            return false;
        }
        return true;
    }

    @Override
    public List<String> getDbTableList() {
        List<String> tables = new ArrayList<>();
        for (String name : mongoDatabase.listCollectionNames()) {
            if (!name.startsWith(tablePrefix)) continue;
            if (name.equals(tablePrefix + "extras")) continue;
            tables.add(name);
        }
        return tables;
    }

    @Override
    public List<String> getBoards() {
        return getDbTableList().stream().map(s -> s.substring(tablePrefix.length())).collect(Collectors.toList());
    }

    @Override
    public String getExtra(UUID id, String placeholder) {
        return Optional.ofNullable(mongoDatabase.getCollection(tablePrefix + "extras")
                        .find(Filters.and(Filters.eq("playerID", id), Filters.eq("placeholder", placeholder)))
                        .first())
                .map(document -> document.getString("value"))
                .orElse(null);
    }

    @Override
    public void upsertExtra(UUID id, String placeholder, String value) {
        Document result = mongoDatabase.getCollection(tablePrefix + "extras")
                .find()
                .filter(Filters.and(Filters.eq("playerID", id), Filters.eq("placeholder", placeholder)))
                .first();
        if (result == null) {
            Document document = new Document();
            document.put("playerID", id);
            document.put("placeholder", placeholder);
            document.put("value", value);
            mongoDatabase.getCollection(tablePrefix + "extras").insertOne(document);
        } else {
            mongoDatabase.getCollection(tablePrefix + "extras").updateOne(Filters.and(Filters.eq("playerID", id), Filters.eq("placeholder", placeholder)),
                    Updates.set("value", value));
        }
    }

    @Override
    public void createExtraTable() {
        mongoDatabase.createCollection(tablePrefix + "extras");
    }

    private boolean isReverse(String board) {
        return storageConfig.getStringList("reverse-sort").contains(board);
    }
}
