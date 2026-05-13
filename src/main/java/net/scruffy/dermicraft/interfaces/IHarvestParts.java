package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.datagen.tag.ModTags;

public interface IHarvestParts {

    default boolean isHarvestable(BlockState state) {
        return state.is(ModTags.Blocks.HARVESTABLE);
    }

    default boolean isHarvestable(Level level, BlockPos pos) {
        return isHarvestable(level.getBlockState(pos));
    }
}
