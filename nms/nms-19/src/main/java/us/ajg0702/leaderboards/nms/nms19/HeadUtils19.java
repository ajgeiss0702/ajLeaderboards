package us.ajg0702.leaderboards.nms.nms19;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import us.ajg0702.leaderboards.nms.legacy.DebugWrapper;
import us.ajg0702.leaderboards.nms.legacy.VersionedHeadUtils;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HeadUtils19 implements VersionedHeadUtils {

    private final DebugWrapper debug;

    public HeadUtils19(DebugWrapper debug) {
        this.debug = debug;
    }

    @Override
    public ItemStack getHeadItem(UUID uuid, String name) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);

        ItemMeta itemMeta = stack.getItemMeta();

        if(!(itemMeta instanceof SkullMeta)) throw new IllegalStateException("Head isnt a skull!");

        SkullMeta meta = (SkullMeta) itemMeta;

        PlayerProfile profile = getProfile(uuid, name);

        meta.setOwnerProfile(profile);

        stack.setItemMeta(meta);

        return stack;
    }

    @Override
    public void setHeadBlock(Block block, UUID uuid, String name) {
        BlockState blockState = block.getState();

        if(!(blockState instanceof Skull)) throw new IllegalArgumentException("Block is not a skull!");

        Skull skull = (Skull) blockState;


        PlayerProfile profile = getProfile(uuid, name);

        skull.setOwnerProfile(profile);

        skull.update();
    }

    private PlayerProfile getProfile(UUID uuid, String name) {
        PlayerProfile profile = Bukkit.getOfflinePlayer(uuid).getPlayerProfile();

        if(profile.getTextures().isEmpty()) {
            debug.infoW("Fetching textures! If this causes lag, contact aj");
            try {
                profile = profile.update().get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }

        debug.infoW("Got profile for " + name + ": " + profile + " with textures" + profile.getTextures());

        return profile;
    }
}
