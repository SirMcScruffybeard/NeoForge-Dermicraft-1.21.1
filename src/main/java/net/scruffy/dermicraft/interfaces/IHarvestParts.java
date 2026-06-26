package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
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
     * Damages the tool used in the given hand
     * @param player
     * @param stack The tool that was used
     * @param wear The durability damage being done to the tool
     * @param hand The hand the tool was used in
     *********************************************************************/
    default void damageTool(Player player, ItemStack stack, int wear, InteractionHand hand) {
        EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(wear, player, slot);
    }
}
