package us.ajg0702.leaderboards.armorstands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

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
										if(eloc.getBlockZ() != curloc.getBlockZ()) {
											//pl.getLogger().info("Skipping east-facing sign bc of wrong block ("+eloc.getBlockX()+" != "+curloc.getBlockX()+", "+eloc.getBlockZ()+" != "+curloc.getBlockZ());
											continue;
										}
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
			meta.setOwningPlayer(player);
		} else {
			meta.setOwner(player.getName());
		}
		item.setItemMeta(meta);
		stand.setHelmet(item);
	}
	
	
	private void checkHead(Location loc, OfflinePlayer player) {
		if (loc.getBlock().getType() != Material.PLAYER_HEAD) return;
		Bukkit.getScheduler().runTask(pl, new Runnable() {
			public void run() {
				
				BlockState bs = loc.getBlock().getState();
				if(!(bs instanceof Skull)) return;
				Skull skull = (Skull) bs;
				
				Bukkit.getScheduler().runTaskAsynchronously(pl, new Runnable() {
					public void run() {
						
						String result = getURLContent("https://api.mojang.com/users/profiles/minecraft/" + player.getName());
						Gson g = new Gson();
					    JsonObject obj = g.fromJson(result, JsonObject.class);
					    String uid = obj.get("id").toString().replace("\"","");
					    String signature = getURLContent("https://sessionserver.mojang.com/session/minecraft/profile/" + uid);
					    obj = g.fromJson(signature, JsonObject.class);
					    String value = obj.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
					    
					    Bukkit.getScheduler().runTaskAsynchronously(pl, new Runnable() {
							public void run() {
							    
						        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
						        profile.getProperties().put("textures", new Property("textures",value));
						        try {
						            Field profileField = skull.getClass().getDeclaredField("profile");
						            profileField.setAccessible(true);
						            profileField.set(skull, profile);
						        }catch (NoSuchFieldException | IllegalAccessException e) { e.printStackTrace(); }
						        skull.update(); // so that the result can be seen
								/*if(VersionSupport.getMinorVersion() > 9) {
									skull.setOwningPlayer(player);
								} else {
									skull.setOwner(player.getName());
								}
								skull.update();*/
							}
						});
					}
				});
				
				
			}
		});
	}
	
	private static String getURLContent(String urlStr) {
        URL url;
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();
        try{
            url = new URL(urlStr);
            in = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8) );
            String str;
            while((str = in.readLine()) != null) {
                sb.append( str );
            }
        } catch (Exception ignored) { }
        finally{
            try{
                if(in!=null) {
                    in.close();
                }
            }catch(IOException ignored) { }
        }
        return sb.toString();
    }
	
}
