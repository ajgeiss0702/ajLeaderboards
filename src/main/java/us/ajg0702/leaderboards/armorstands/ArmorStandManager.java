package us.ajg0702.leaderboards.armorstands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import us.ajg0702.leaderboards.Main;
import us.ajg0702.leaderboards.signs.BoardSign;
import us.ajg0702.utils.spigot.VersionSupport;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

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
	
	public void search(BoardSign sign, String name, UUID id) {
		if(!sign.getLocation().getBlock().getType().toString().contains("SIGN")) return;
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
			org.bukkit.material.Sign bs = (org.bukkit.material.Sign) ss.getData();
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
					debugParticles(curloc);

					checkHead(curloc, name, id);
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
					debugParticles(curloc);
					
					checkHead(curloc, name, id);
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
					debugParticles(curloc);

					checkHead(curloc, name, id);
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
					debugParticles(curloc);

					checkHead(curloc, name, id);
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

	@SuppressWarnings("deprecation")
	private void setArmorstandHead(ArmorStand stand, String name, UUID id) {
		//pl.getLogger().info("in armorstand");
		if(VersionSupport.getMinorVersion() >= 10) {
			stand.setSilent(true);
		}
		ItemStack item = null;
		if(VersionSupport.getMinorVersion() <= 12) {
			item = new ItemStack(Material.valueOf("SKULL_ITEM"), 1 , (short) 3);
		} else if(VersionSupport.getMinorVersion() > 12) {
			item = new ItemStack(Material.PLAYER_HEAD, 1);
		}
		//pl.getLogger().info("item");
		if(item == null) return;
		//pl.getLogger().info("item not null");
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		if(VersionSupport.getMinorVersion() > 9) {
			meta.setOwningPlayer(Bukkit.getOfflinePlayer(id));
		} else {
			meta.setOwner(name);
		}
		item.setItemMeta(meta);
		stand.setHelmet(item);
	}

	private void debugParticles(Location curloc) {
		//curloc.getWorld().spawnParticle(Particle.FLAME, curloc.add(0.5, 0.5, 0.5).toVector().toLocation(curloc.getWorld()), 20, 0.25, 0.25, 0.25, 0);
	}
	
	
	private void checkHead(Location loc, String name, UUID id) {
		Bukkit.getScheduler().runTask(pl, new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				BlockState bs = loc.getBlock().getState();
				if(!(bs instanceof Skull)) return;
				Skull skull = (Skull) bs;
				boolean update = false;
				if(VersionSupport.getMinorVersion() > 9) {
					if(!Objects.equals(skull.getOwningPlayer().getUniqueId(), id)) {
						skull.setOwningPlayer(Bukkit.getOfflinePlayer(id));
						update = true;
					}
				} else {
					if(!Objects.equals(skull.getOwner(), name)) {
						skull.setOwner(name);
						update = true;
					}
				}
				if(update) skull.update();
			}
		});
	}

	private void checkArmorstand(Location curloc, String name, UUID id) {
		Collection<Entity> entities = curloc.getWorld().getNearbyEntities(curloc, 1, 1, 1);
		if(entities.size() > 0) {
			Bukkit.getScheduler().runTaskAsynchronously(pl, new Runnable() {
				public void run() {
					for(Entity entity : entities) {
						if(entity instanceof ArmorStand) {
							Location eloc = entity.getLocation();
							if(eloc.getBlockX() != curloc.getBlockX() || eloc.getBlockZ() != curloc.getBlockZ()) continue;
							setArmorstandHead((ArmorStand) entity, name, id);
						}
					}
				}
			});
		}
	}
	
}
