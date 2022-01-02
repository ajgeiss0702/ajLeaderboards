package us.ajg0702.leaderboards.displays.heads;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.displays.signs.BoardSign;
import us.ajg0702.utils.spigot.VersionSupport;

import java.util.HashMap;
import java.util.UUID;

public class HeadManager {
    private final LeaderboardPlugin plugin;

    public HeadManager(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    public void search(BoardSign sign, String name, UUID id) {
        if(!sign.getLocation().getBlock().getType().toString().contains("SIGN")) return;
        if(!plugin.getCache().boardExists(sign.getBoard())) return;
        if(id == null) return;
        Sign ss = sign.getSign();
        BlockFace face;
        if(VersionSupport.getMinorVersion() > 12) {
            BlockData bd = ss.getBlockData();
            if(bd instanceof org.bukkit.block.data.type.Sign) {
                org.bukkit.block.data.type.Sign bs = (org.bukkit.block.data.type.Sign) bd;
                face = bs.getRotation();
            } else {
                WallSign bs = (WallSign) bd;
                face = bs.getFacing();
            }
        } else {
            @SuppressWarnings("deprecation") org.bukkit.material.Sign bs = (org.bukkit.material.Sign) ss.getData();
            face = bs.getFacing();
        }

        Location sl = sign.getLocation();

        //pl.getLogger().info(face.toString());

        switch(face) {

            case NORTH:
            case NORTH_EAST:
            case NORTH_NORTH_EAST:
            case NORTH_NORTH_WEST:
            case NORTH_WEST:

                for(int z = sl.getBlockZ()+1;z > sl.getBlockZ()-1;z--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), sl.getX(), y, z);
                        checkHead(curloc, name, id);
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
                        checkHead(curloc, name, id);
                    }
                }
                break;

            case EAST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:

                for(int x = sl.getBlockX();x > sl.getBlockX()-2;x--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), x, y, sl.getZ());
                        checkHead(curloc, name, id);
                    }
                }
                break;

            case WEST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:


                for(int x = sl.getBlockX()+1;x > sl.getBlockX()-1;x--) {
                    for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
                        Location curloc = new Location(sl.getWorld(), x, y, sl.getZ());
                        checkHead(curloc, name, id);
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

    private final HashMap<Location, UUID> headLocationCache = new HashMap<>();

    public void checkHead(Location loc, String name, UUID id) {
        Validate.notNull(loc);
        Validate.notNull(id, "UUID is null!");

        if(id.equals(headLocationCache.get(loc))) return;

        OfflinePlayer op = VersionSupport.getMinorVersion() > 9 ? Bukkit.getOfflinePlayer(id) : null;

        Bukkit.getScheduler().runTask(plugin, () -> {
            BlockState bs = loc.getBlock().getState();
            if(!(bs instanceof Skull)) return;

            Skull skull = (Skull) bs;
            if(VersionSupport.getMinorVersion() > 9) {
                assert op != null;
                skull.setOwningPlayer(op);
            } else {
                //noinspection deprecation
                skull.setOwner(name);
            }
            skull.update();
            headLocationCache.put(loc, id);
        });
    }
}
