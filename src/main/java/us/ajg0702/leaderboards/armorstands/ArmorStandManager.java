package us.ajg0702.leaderboards.armorstands;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import us.ajg0702.leaderboards.Main;
import us.ajg0702.leaderboards.signs.BoardSign;
import us.ajg0702.utils.spigot.VersionSupport;

public class ArmorStandManager {
	static ArmorStandManager instance;
	public static ArmorStandManager getInstance() {
		return instance;
	}
	public static ArmorStandManager getInstance(Main pl) {
		if(instance == null) {
			instance = new ArmorStandManager(pl);
		}
		return instance;
	}
	
	
	Main pl;
	
	private ArmorStandManager(Main pl) {
		this.pl = pl;
	}
	
	
	// E/W = +/- x
	// N/S = +/- z
	
	public void search(BoardSign sign, OfflinePlayer player) { 
		if(!sign.getLocation().getBlock().getType().toString().contains("SIGN")) return;
		Sign ss = sign.getSign();
		org.bukkit.material.Sign bs = (org.bukkit.material.Sign) ss.getData();
		BlockFace face = bs.getFacing();
		Location sl = sign.getLocation();
		
		//pl.getLogger().info(face.toString());
		
		switch(face) {
			
		case NORTH:
		case NORTH_EAST:
		case NORTH_NORTH_EAST:
		case NORTH_NORTH_WEST:
		case NORTH_WEST:
		case SOUTH:
		case SOUTH_EAST:
		case SOUTH_SOUTH_EAST:
		case SOUTH_SOUTH_WEST:
		case SOUTH_WEST:
			
			for(int z = sl.getBlockZ()+1;z > sl.getBlockZ()-1;z--) {
				for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
					Location curloc = new Location(sl.getWorld(), sl.getX(), y, z);
					/*if(!curloc.getBlock().getType().toString().contains("SIGN")) {
						curloc.getBlock().setType(Material.STONE);
					}
					//Bukkit.getPlayer("ajgeiss0702").teleport(curloc);*/
					
					checkHead(curloc, player);
					
					Collection<Entity> entities = sl.getWorld().getNearbyEntities(curloc, 1, 1, 1);
					if(entities.size() > 0) {
						Bukkit.getScheduler().runTaskAsynchronously(pl, new Runnable() {
							public void run() {
								for(Entity entity : entities) {
									if(entity instanceof ArmorStand) {
										Location eloc = entity.getLocation();
										if(eloc.getBlockX() != curloc.getBlockX() || eloc.getBlockZ() != curloc.getBlockZ()) continue;
										setArmorstandHead((ArmorStand) entity, player);
									}
								}
							}
						});
					}
				}
			}
			break;
			
		case EAST:
		case EAST_NORTH_EAST:
		case EAST_SOUTH_EAST:
		case WEST:
		case WEST_NORTH_WEST:
		case WEST_SOUTH_WEST:
			
			
			for(int x = sl.getBlockX()+1;x > sl.getBlockX()-1;x--) {
				for(int y = sl.getBlockY()+1;y > sl.getBlockY()-1;y--) {
					Location curloc = new Location(sl.getWorld(), x, y, sl.getZ());
					/*if(!curloc.getBlock().getType().toString().contains("SIGN")) {
						curloc.getBlock().setType(Material.STONE);
					}
					//Bukkit.getPlayer("ajgeiss0702").teleport(curloc);*/
					
					checkHead(curloc, player);
					
					Collection<Entity> entities = sl.getWorld().getNearbyEntities(curloc, 1, 1, 1);
					if(entities.size() > 0) {
						Bukkit.getScheduler().runTaskAsynchronously(pl, new Runnable() {
							public void run() {
								for(Entity entity : entities) {
									if(entity instanceof ArmorStand) {
										Location eloc = entity.getLocation();
										if(eloc.getBlockX() != curloc.getBlockX() || eloc.getBlockZ() != curloc.getBlockZ()) continue;
										setArmorstandHead((ArmorStand) entity, player);
									}
								}
							}
						});
					}
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
	
	@SuppressWarnings("deprecation")
	private void setArmorstandHead(ArmorStand stand, OfflinePlayer player) {
		pl.getLogger().info("in armorstand");
		stand.setSilent(true);
		ItemStack item = null;
		if(VersionSupport.getMinorVersion() <= 12) {
			item = new ItemStack(Material.valueOf("SKULL_ITEM"), 1 , (short) 3);
		} else if(VersionSupport.getMinorVersion() > 12) {
			item = new ItemStack(Material.PLAYER_HEAD, 1);
		}
		pl.getLogger().info("item");
		if(item == null) return;
		pl.getLogger().info("item not null");
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		if(VersionSupport.getMinorVersion() > 9) {
			meta.setOwningPlayer(player);
		} else {
			meta.setOwner(player.getName());
		}
		item.setItemMeta(meta);
		stand.setHelmet(item);
	}
	
	
	private void checkHead(Location loc, OfflinePlayer player) {
		Bukkit.getScheduler().runTask(pl, new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				BlockState bs = loc.getBlock().getState();
				if(!(bs instanceof Skull)) return;
				Skull skull = (Skull) bs;
				if(VersionSupport.getMinorVersion() > 9) {
					skull.setOwningPlayer(player);
				} else {
					skull.setOwner(player.getName());
				}
				skull.update();
			}
		});
	}
	
}
