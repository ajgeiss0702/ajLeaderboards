package us.ajg0702.leaderboards.nms.nms19;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import us.ajg0702.leaderboards.nms.legacy.VersionedHeadUtils;

import java.util.UUID;

public class HeadUtils19 implements VersionedHeadUtils {

    @Override
    public ItemStack getHeadItem(UUID uuid, String name) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);

        ItemMeta itemMeta = stack.getItemMeta();

        if(!(itemMeta instanceof SkullMeta)) throw new IllegalStateException("Head isnt a skull!");

        SkullMeta meta = (SkullMeta) itemMeta;

        meta.setOwnerProfile(Bukkit.createPlayerProfile(uuid, name));

        stack.setItemMeta(meta);

        return stack;
    }

    @Override
    public void setHeadBlock(Block block, UUID uuid, String name) {
        BlockState blockState = block.getState();

        if(!(blockState instanceof Skull)) throw new IllegalArgumentException("Block is not a skull!");

        Skull skull = (Skull) blockState;

        skull.setOwnerProfile(Bukkit.createPlayerProfile(uuid, name));

        skull.update();
    }
}
