package us.ajg0702.leaderboards.nms.legacy;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface VersionedHeadUtils {
    ItemStack getHeadItem(UUID uuid, String name);

    void setHeadBlock(Block block, UUID uuid, String name);
}
