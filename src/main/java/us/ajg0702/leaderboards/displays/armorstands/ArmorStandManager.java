package us.ajg0702.leaderboards.displays.armorstands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.displays.signs.BoardSign;
import us.ajg0702.utils.spigot.VersionSupport;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class ArmorStandManager {

    private final LeaderboardPlugin plugin;

    public ArmorStandManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    private final HashMap<Location, ArmorStandCache> armorStandCache = new HashMap<>();

    private void checkArmorstand(BoardSign sign, Location loc, String name, UUID id) throws ExecutionException, InterruptedException, TimeoutException {
        ArmorStandCache cache = armorStandCache.get(sign.getLocation());

        // If the cache was found, just update the head
        if (cache != null) {
            ArmorStand cacheEntity = cache.getEntity();
            if (cacheEntity != null && !cacheEntity.isDead()) {
                if (id != null && id.equals(cache.getId())) return;
                cache.setId(id);
                setArmorstandHead(cacheEntity, name, id);
                return;
            }
        }

        // The cache was not found. Find an armorstand and add it to the cache
        if (plugin.isShuttingDown()) return;
        for (Entity entity : getNearbyEntities(loc).get(2, TimeUnit.SECONDS)) {
            if (!(entity instanceof ArmorStand)) continue;
            Location entityLoc = entity.getLocation();
            if (entityLoc.getBlockX() != loc.getBlockX() || entityLoc.getBlockZ() != loc.getBlockZ()) continue;
            ArmorStand armorStand = (ArmorStand) entity;
            armorStandCache.put(sign.getLocation(), new ArmorStandCache(armorStand));
            break;
        }
    }

    private void setArmorstandHead(ArmorStand stand, String name, UUID uuid) {
        Debug.info("Updating armorstand");
        if(VersionSupport.getMinorVersion() >= 10) {
            stand.setSilent(true);
        }
        ItemStack item;
        if(plugin.getHeadUtils().getVersionedHeadUtils() != null) {
            item = plugin.getHeadUtils().getVersionedHeadUtils().getHeadItem(uuid, name);
        } else {
            item = plugin.getHeadUtils().getHeadItem(uuid, name);
        }
        //noinspection deprecation
        stand.setHelmet(item);
    }

    public void search(BoardSign sign, String name, UUID id) {
        if(!sign.getLocation().getBlock().getType().toString().contains("SIGN")) return;
        if(!plugin.getTopManager().getBoards().contains(sign.getBoard())) return;
        if(id == null) return;
        Sign ss = sign.getSign();
        if(ss == null) return;
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

        try {
            switch(face) {

                case NORTH:
                case NORTH_EAST:
                case NORTH_NORTH_EAST:
                case NORTH_NORTH_WEST:
                case NORTH_WEST:

                    for(int z = sl.getBlockZ()+1;z > sl.getBlockZ()-1;z--) {
                        for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                            Location curloc = new Location(sl.getWorld(), sl.getX(), y, z);
                            checkArmorstand(sign, curloc, name, id);
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
                            checkArmorstand(sign, curloc, name, id);
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
                            checkArmorstand(sign, curloc, name, id);
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
                            checkArmorstand(sign, curloc, name, id);
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
        } catch(ExecutionException | InterruptedException e) {
            if(plugin.isShuttingDown()) return;
            plugin.getLogger().log(Level.WARNING, "Interupted while scanning for armorstand:", e);
        } catch(TimeoutException ignored) {}
    }

    private Future<Collection<Entity>> getNearbyEntities(Location loc) {
        CompletableFuture<Collection<Entity>> future = new CompletableFuture<>();
        World world = loc.getWorld();
        if(world == null) {
            throw new IllegalArgumentException("Invalid world");
        }
        Bukkit.getScheduler().runTask(
                plugin,
                () -> future.complete(world.getNearbyEntities(loc, 1, 1, 1))
        );
        return future;
    }
    public static void debugParticles(Location curloc) {
        if(!Debug.particles()) return;
        World world = curloc.getWorld();
        if(world == null) return;
        world.spawnParticle(Particle.FLAME, curloc.add(0.5, 0.5, 0.5).toVector().toLocation(curloc.getWorld()), 20, 0.25, 0.25, 0.25, 0);
    }
}
