package us.ajg0702.leaderboards.displays.armorstands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class ArmorStandManager {

    private final LeaderboardPlugin plugin;

    public ArmorStandManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    HashMap<Location, ArmorStandCache> armorStandCache = new HashMap<>();

    private void checkArmorstand(Location loc, String name, UUID id) {
        Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, 1, 1, 1);
        if(entities.size() <= 0) return;
        for(Entity entity : entities) {
            if(entity instanceof ArmorStand) {
                Location eloc = entity.getLocation();
                if(eloc.getBlockX() != loc.getBlockX() || eloc.getBlockZ() != loc.getBlockZ()) continue;

                ArmorStandCache cache = armorStandCache.get(eloc);
                if(cache != null) {
                    if(!cache.getEntity().equals(entity)) return;
                    if(cache.getId().equals(id)) return;
                }

                armorStandCache.put(eloc, new ArmorStandCache(loc, entity, id));

                setArmorstandHead((ArmorStand) entity, name, id);
            }
        }
    }

    private void setArmorstandHead(ArmorStand stand, String name, UUID id) {
        if(VersionSupport.getMinorVersion() >= 10) {
            stand.setSilent(true);
        }
        ItemStack item = plugin.getHeadUtils().getHeadItem(name);
        stand.setHelmet(item);
    }

    public void search(BoardSign sign, String name, UUID id) {
        if(!sign.getLocation().getBlock().getType().toString().contains("SIGN")) return;
        if(!plugin.getCache().boardExists(sign.getBoard())) return;
        if(id == null) return;
        Sign ss = sign.getSign();
        BlockFace face;
        if(VersionSupport.getMinorVersion() > 12) {
            BlockData bd = ss.getBlockData();
            if(bd instanceof Sign) {
                org.bukkit.block.data.type.Sign bs = (org.bukkit.block.data.type.Sign) bd;
                face = bs.getRotation();
            } else {
                WallSign bs = (WallSign) bd;
                face = bs.getFacing();
            }
        } else {
            org.bukkit.material.Sign bs = (org.bukkit.material.Sign) ss.getData();
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
                        checkArmorstand(curloc, name, id);
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
                        checkArmorstand(curloc, name, id);
                    }
                }
                break;

            case EAST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:

                for(int x = sl.getBlockX();x > sl.getBlockX()-2;x--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), x, y, sl.getZ());
                        checkArmorstand(curloc, name, id);
                    }
                }
                break;

            case WEST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:


                for(int x = sl.getBlockX()+1;x > sl.getBlockX()-1;x--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), x, y, sl.getZ());
                        checkArmorstand(curloc, name, id);
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
