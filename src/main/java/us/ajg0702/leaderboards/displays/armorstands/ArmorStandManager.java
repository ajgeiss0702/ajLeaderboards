package us.ajg0702.leaderboards.displays.armorstands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.displays.signs.BoardSign;
import us.ajg0702.utils.spigot.VersionSupport;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static us.ajg0702.leaderboards.displays.heads.HeadUtils.debugParticles;

public class ArmorStandManager {

    private final LeaderboardPlugin plugin;

    public ArmorStandManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::executeQueue, 10, 10);
    }

    private final HashMap<Location, ArmorStandCache> armorStandCache = new HashMap<>();
    private final Queue<ArmorStandRequest> requestQueue = new LinkedList<>();
    private final AtomicBoolean waiting = new AtomicBoolean();

    private void executeQueue() {
        if (waiting.get()) return;
        ArmorStandRequest request = requestQueue.poll();
        if (request == null) return;
        BoardSign sign = request.getSign();
        UUID id = request.getId();
        String name = request.getName();

        ArmorStandCache cache = armorStandCache.get(sign.getLocation());

        // If the cache was found, just update the head
        if (cache != null) {
            ArmorStand cacheEntity = cache.getEntity();
            if (cacheEntity != null && !cacheEntity.isDead()) {
                if (Objects.equals(cache.getId(), id)) return;
                cache.setId(id);
                setArmorstandHead(cacheEntity, name, id);
                return;
            }
        }

        // The cache was not found. Find an armorstand and add it to the cache
        Location loc = request.getLocation();
        World world = loc.getWorld();
        assert world != null;
        waiting.set(true);
        if (plugin.isShuttingDown()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Entity entity : world.getNearbyEntities(loc, 1, 1, 1)) {
                if (entity instanceof ArmorStand) {
                    Location entityLoc = entity.getLocation();
                    if (entityLoc.getBlockX() != loc.getBlockX() || entityLoc.getBlockZ() != loc.getBlockZ()) continue;
                    ArmorStand armorStand = (ArmorStand) entity;
                    armorStandCache.put(sign.getLocation(), new ArmorStandCache(armorStand));
                    requestQueue.add(request);
                    break;
                }
            }
            waiting.set(false);
        });
    }

    private void addRequest(BoardSign sign, Location loc, String name, UUID id) {
        requestQueue.add(new ArmorStandRequest(sign, loc, name, id));
    }

    private void setArmorstandHead(ArmorStand stand, String name, UUID id) {
        if(VersionSupport.getMinorVersion() >= 10) {
            stand.setSilent(true);
        }
        ItemStack item = plugin.getHeadUtils().getHeadItem(name);
        //noinspection deprecation
        stand.setHelmet(item);
    }

    public void search(BoardSign sign, String name, UUID id) {
        if(!sign.getLocation().getBlock().getType().toString().contains("SIGN")) return;
        if(!plugin.getTopManager().getBoards().contains(sign.getBoard())) return;
        if(id == null) return;
        Sign ss = sign.getSign();
        BlockFace face;
        if(VersionSupport.getMinorVersion() > 12) {
            BlockData bd = ss.getBlockData();
            if(bd instanceof org.bukkit.block.data.type.Sign) {
                org.bukkit.block.data.type.Sign bs = (org.bukkit.block.data.type.Sign) bd;
                face = bs.getRotation();
            } else if(bd instanceof WallSign) {
                WallSign bs = (WallSign) bd;
                face = bs.getFacing();
            } else {
                plugin.getLogger().warning("nope");
                return;
            }
        } else {
            @SuppressWarnings("deprecation") org.bukkit.material.Sign bs = (org.bukkit.material.Sign) ss.getData();
            face = bs.getFacing();
        }

        Location sl = sign.getLocation();

        switch(face) {

            case NORTH:
            case NORTH_EAST:
            case NORTH_NORTH_EAST:
            case NORTH_NORTH_WEST:
            case NORTH_WEST:

                for(int z = sl.getBlockZ()+1;z > sl.getBlockZ()-1;z--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), sl.getX(), y, z);
                        addRequest(sign, curloc, name, id);
                        debugParticles(curloc);
                    }
                }
                break;

            case SOUTH:
            case SOUTH_EAST:
            case SOUTH_SOUTH_EAST:
            case SOUTH_SOUTH_WEST:
            case SOUTH_WEST:

                for(int z = sl.getBlockZ();z > sl.getBlockZ()-2;z--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), sl.getX(), y, z);
                        addRequest(sign, curloc, name, id);
                        debugParticles(curloc);
                    }
                }
                break;

            case EAST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:

                for(int x = sl.getBlockX();x > sl.getBlockX()-2;x--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), x, y, sl.getZ());
                        addRequest(sign, curloc, name, id);
                        debugParticles(curloc);
                    }
                }
                break;

            case WEST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:


                for(int x = sl.getBlockX()+1;x > sl.getBlockX()-1;x--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), x, y, sl.getZ());
                        addRequest(sign, curloc, name, id);
                        debugParticles(curloc);
                    }
                }
                break;

            case DOWN:
            case UP:
            case SELF:
            default:
                break;

        }
    }
}
