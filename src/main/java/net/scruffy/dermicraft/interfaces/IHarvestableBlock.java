package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.ModBlocks;
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
    List<ItemStack> harvest(Level level, Player player, ItemStack stack, BlockPos pos);

    default boolean isHarvester(ItemStack stack) {
        return stack.is(ModTags.Items.EXTRACTION_TOOLS);
    }



    default ItemStack getSingleTypeHarvest(Level level, Item item) {
        RandomSource random = level.getRandom();
        int count = getDropCount(random, 2, 5);

        if (level.random.nextFloat() < 0.20f) {
            count++;
        }
        return new ItemStack(item, count);
    }

    default void playHarvestSound(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, volume, pitch);
    }

    default void playDefaultHarvestSound(Level level, BlockPos pos) {
        playHarvestSound(level, pos, SoundEvents.SLIME_BLOCK_BREAK, 1.0F, 0.8F);
    }

    default void displayDefaultHarvestParticles(Level level, BlockPos pos, BlockState oldState) {
        level.levelEvent(2001, pos, Block.getId(oldState));
    }

    default int getDropCount(RandomSource random, int origin, int bound) {
        return random.nextInt(2, 5);
    }

    default void changeToMarredTumor(Level level, BlockPos pos, BlockState oldState) {
        level.setBlock(pos, ModBlocks.MARRED_TUMOR.get().defaultBlockState(), 3);

        playDefaultHarvestSound(level, pos);
        displayDefaultHarvestParticles(level, pos, oldState);
    }
}
