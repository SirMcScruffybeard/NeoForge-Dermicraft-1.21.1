package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ModItemUtil {
    public static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void giveItems(Player player, List<ItemStack> stackList) {
        for (ItemStack dropItem : stackList) {
            giveItem(player, dropItem);
        }
    }

    /*********************************************************************************************************
     *
     * @param level
     * @param player
     * @param pos
     * @param state
     * @param tool
     * Gives the player the items dropped from a block entity instead of dropping it on the ground
     *********************************************************************************************************/
    public static void giveDrops(Level level, Player player, BlockPos pos, BlockState state, ItemStack tool) {
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, level.getBlockEntity(pos), player, tool);

        giveItems(player, drops);
    }
}
