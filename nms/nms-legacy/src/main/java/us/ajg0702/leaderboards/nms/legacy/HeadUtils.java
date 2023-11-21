package us.ajg0702.leaderboards.nms.legacy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
//import net.skinsrestorer.api.SkinsRestorerAPI;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import us.ajg0702.utils.spigot.VersionSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class HeadUtils {


    /**public void setSkullSkin (String name, Block block) {
     if(!(block.getState() instanceof Skull)) return;
     Skull skullData = (Skull)block.getState();
     // : figure out how to do this without nms
     TileEntitySkull skullTile = (TileEntitySkull)((CraftWorld)block.getWorld()).getHandle().getTileEntity(new BlockPosition(block.getX(), block.getY(), block.getZ()));
     skullTile.setGameProfile(getNonPlayerProfile(value));
     block.getState().update(true);
     }**/

//    private final SkinsRestorerAPI skinsRestorerAPI;

    private VersionedHeadUtils versionedHeadUtils = null;

    private final Logger logger;

    public HeadUtils(Logger logger) {
        this.logger = logger;

        /*if(Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            skinsRestorerAPI = SkinsRestorerAPI.getApi();
        } else {
            skinsRestorerAPI = null;
        }*/

        if(VersionSupport.getMinorVersion() >= 19) {
            try {
                versionedHeadUtils = (VersionedHeadUtils) Class.forName("us.ajg0702.leaderboards.nms.nms19.HeadUtils19")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException | ClassNotFoundException e) {
                logger.warning("Unable to find 1.19 nms class: "+e.getMessage());
            }
        }
    }

    public VersionedHeadUtils getVersionedHeadUtils() {
        return versionedHeadUtils;
    }

    public ItemStack getHeadItem(UUID uuid, String name) {

        ItemStack skull = null;
        if(VersionSupport.getMinorVersion() <= 12) {
            //noinspection deprecation
            skull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1 , (short) 3);
        } else if(VersionSupport.getMinorVersion() > 12) {
            skull = new ItemStack(Material.PLAYER_HEAD, 1);
        }
        String value;
        //if(skinsRestorerAPI == null) {
            if(uuid != null && Bukkit.getOnlineMode()) {
                value = getHeadValue(uuid);
            } else {
                value = getHeadValue(name);
            }
        /*} else {
            IProperty profile = skinsRestorerAPI.getProfile(uuid.toString());
            if(profile == null) {
                logger.warning("SkinsRestorer profile for "+uuid+" is null!");
                return skull;
            }
            value = profile.getValue();
        }*/
        if(value.equals("")) return skull;
        UUID hashAsId = new UUID(value.hashCode(), value.hashCode());
        //noinspection deprecation
        return Bukkit.getUnsafe().modifyItemStack(
                skull,
                "{SkullOwner:{Id:\"" + hashAsId + "\",Properties:{textures:[{Value:\"" + value + "\"}]}}}"
        );
    }


    Map<String, CachedData<String>> skinCache = new HashMap<>();
    final long lastClear = System.currentTimeMillis();

    public String getHeadValue(String nameOrUUID) {

        if(System.currentTimeMillis() - lastClear > 5400e3) { // completely wipe the cache every hour and a half
            skinCache = new HashMap<>();
        }

        if(skinCache.containsKey(nameOrUUID) && skinCache.get(nameOrUUID).getTimeSince() < 300e3) {
            return skinCache.get(nameOrUUID).getData();
        }

        String profile = getURLContent("https://api.ashcon.app/mojang/v2/user/" + nameOrUUID);
        if(profile.isEmpty()) return "";
        Gson g = new Gson();
        JsonObject jObj = g.fromJson(profile, JsonObject.class);
        if(jObj == null || jObj.get("textures") == null) return "";
        String url = jObj.getAsJsonObject("textures").getAsJsonObject("skin").get("url").getAsString();
//        String decoded = new String(Base64.getDecoder().decode(value));
//        jObj = g.fromJson(decoded, JsonObject.class);
//        String skin = jObj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        byte[] skinByte = ("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes();
        String finalSkin = new String(Base64.getEncoder().encode(skinByte));
        skinCache.put(nameOrUUID, new CachedData<>(finalSkin));
        return finalSkin;
    }

    public String getHeadValue(UUID uuid) {
        return getHeadValue(uuid.toString());
    }


    public static GameProfile getNonPlayerProfile(String skinURL) {
        GameProfile newSkinProfile = new GameProfile(UUID.randomUUID(), null);
        newSkinProfile.getProperties().put(
                "textures",
                new Property(
                        "textures",
                        Base64Coder.encodeString("{textures:{SKIN:{url:\"" + skinURL + "\"}}}")
                )
        );
        return newSkinProfile;
    }


    private final OkHttpClient httpClient = new OkHttpClient();

    final HashMap<String, String> urlCache = new HashMap<>();
    final HashMap<String, Long> urlLastget = new HashMap<>();
    private String getURLContent(String urlStr) {
        if(
                urlLastget.containsKey(urlStr) &&
                        System.currentTimeMillis() - urlLastget.get(urlStr) < 300e3 // Cache for 5 minutes
        ) {
            return urlCache.get(urlStr);
        }
        Request request = new Request.Builder()
                .url(urlStr)
                .build();



        try(Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if(!response.isSuccessful() || body == null) {
                /*if(body != null) {
                    logger.info("Unsuccessful (" + response.code() + ") with: " + body.string());
                } else {
                    logger.info("Null body with " + response.code());
                }*/
                return urlCache.getOrDefault(urlStr, "");
            }
            String r = body.string();
            urlCache.put(urlStr, r);
            urlLastget.put(urlStr, System.currentTimeMillis());
            return r;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
