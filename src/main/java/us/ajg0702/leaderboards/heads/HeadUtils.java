package us.ajg0702.leaderboards.heads;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import us.ajg0702.utils.spigot.VersionSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

public class HeadUtils {
    static HeadUtils instance;
    public static HeadUtils getInstance() {
        if(instance == null) {
            instance = new HeadUtils();
        }
        return instance;
    }


    /**public void setSkullSkin (String name, Block block) {
        if(!(block.getState() instanceof Skull)) return;
        Skull skullData = (Skull)block.getState();
        // TODO: figure out how to do this without nms
        TileEntitySkull skullTile = (TileEntitySkull)((CraftWorld)block.getWorld()).getHandle().getTileEntity(new BlockPosition(block.getX(), block.getY(), block.getZ()));
        skullTile.setGameProfile(getNonPlayerProfile(value));
        block.getState().update(true);
    }**/


    public ItemStack getHeadItem(String name) {
        ItemStack skull = null;
        if(VersionSupport.getMinorVersion() <= 12) {
            skull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1 , (short) 3);
        } else if(VersionSupport.getMinorVersion() > 12) {
            skull = new ItemStack(Material.PLAYER_HEAD, 1);
        }
        String value = getHeadValue(name);
        UUID hashAsId = new UUID(value.hashCode(), value.hashCode());
        return Bukkit.getUnsafe().modifyItemStack(
                skull,
                "{SkullOwner:{Id:\"" + hashAsId + "\",Properties:{textures:[{Value:\"" + value + "\"}]}}}"
        );
    }


    public String getHeadValue(String name){
        String result = getURLContent("https://api.mojang.com/users/profiles/minecraft/" + name);

        Gson g = new Gson();
        JsonObject jObj = g.fromJson(result, JsonObject.class);
        String uuid = jObj.get("id").toString().replace("\"","");
        String signature = getURLContent("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        jObj = g.fromJson(signature, JsonObject.class);
        String value = jObj.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
        String decoded = new String(Base64.getDecoder().decode(value));
        jObj = g.fromJson(decoded, JsonObject.class);
        String skin = jObj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        byte[] skinByte = ("{\"textures\":{\"SKIN\":{\"url\":\"" + skin + "\"}}}").getBytes();
        return new String(Base64.getEncoder().encode(skinByte));
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


    HashMap<String, String> urlCache = new HashMap<>();
    HashMap<String, Long> urlLastget = new HashMap<>();
    private String getURLContent(String urlStr) {
        if(
                urlLastget.containsKey(urlStr) &&
                System.currentTimeMillis() - urlLastget.get(urlStr) < 300e3 // Cache for 5 minutes
        ) {
            return urlCache.get(urlStr);
        }
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

        String r = sb.toString();
        urlCache.put(urlStr, r);
        urlLastget.put(urlStr, System.currentTimeMillis());
        return r;
    }
}
