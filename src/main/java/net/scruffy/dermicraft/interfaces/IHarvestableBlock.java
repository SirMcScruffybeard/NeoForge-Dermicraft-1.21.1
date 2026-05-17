package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.datagen.tag.ModTags;

import java.util.List;

public interface IHarvestableBlock {

    /********************************************************************************
     * Handles the transition from the whole state to the harvested/marred state.
     * @param level The level where the block resides.
     * @param pos The position of the block.
     * @param state The current state of the block before changing.
     ********************************************************************************/
    void changeState(Level level, BlockPos pos, BlockState state);

    /********************************************************************************
     * @param player The player performing the harvest.
     * @param stack  The item stack being used (must implement IHarvestParts).
     * @param level  The level context.
     * @param pos    The position of the block.
     * @return A list of ItemStacks harvested from the block.
     *********************************************************************************/
    List<ItemStack> harvest(Level level, Player player, ItemStack stack,  BlockPos pos);

    default boolean isHarvester(ItemStack stack) {
        return stack.is(ModTags.Items.HARVEST_TOOLS);
    }
}
