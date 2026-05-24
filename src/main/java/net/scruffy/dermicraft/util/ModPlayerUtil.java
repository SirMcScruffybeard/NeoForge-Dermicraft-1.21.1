package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ModPlayerUtil {

    public static void giveItem(Player player, ItemStack stack){
        ModItemUtil.giveItem(player, stack);
    }

    public static void giveItems(Player player, List<ItemStack> stackList){
        ModItemUtil.giveItems(player, stackList);
    }

    public static void emptyHand(Player player, InteractionHand hand) {
        if (player != null && hand != null) {
            if (!player.level().isClientSide) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
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
        ModItemUtil.giveDrops(level, player, pos, state, tool);
    }

    public static boolean isFullHealth(Player player) {
        return (player.getHealth() >= player.getMaxHealth());
    }
}
