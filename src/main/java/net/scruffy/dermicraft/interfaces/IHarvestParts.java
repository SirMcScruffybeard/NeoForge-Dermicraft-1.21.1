package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.datagen.tag.ModTags;

public interface IHarvestParts {

    default boolean isHarvestable(BlockState state) {
        return state.is(ModTags.Blocks.EXTRACTABLE);
    }

    default boolean isHarvestable(Level level, BlockPos pos) {
        return isHarvestable(level.getBlockState(pos));
    }

    /********************************************************************
     * Damages the tool used in the player's main hand
     * @param player
     * @param stack The tool in the main hand
     * @param wear The durability damage being done to the tool
     *********************************************************************/
    default void damageTool(Player player, ItemStack stack, int wear) {
        stack.hurtAndBreak(wear, player, EquipmentSlot.MAINHAND);
    }
}
