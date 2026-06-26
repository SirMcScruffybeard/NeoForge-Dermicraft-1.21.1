package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.util.ModItemUtil;

public interface ICollectBlocks {

    void collect(Level level, BlockPos pos, Player player);

    default boolean canCollect(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.COLLECTIBLE);
    }

    default void changeToAir(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    default void grabItem(Player player, ItemStack stack) {
        ModItemUtil.giveItem(player, stack);
    }

    default ItemStack getBlockItem(Level level, BlockPos pos) {
        return new ItemStack(level.getBlockState(pos).getBlock().asItem());
    }

    default void playPickupSound(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    default void playDefaultPickupSound(Level level, BlockPos pos) {
        playPickupSound(level, pos, SoundEvents.SCULK_BLOCK_BREAK, 1f, 0.5f);
    }

}
