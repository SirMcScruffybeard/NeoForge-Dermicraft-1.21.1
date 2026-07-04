package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.util.ModItemUtil;

/**
 * Grants an item the ability to pick up Dermicraft's "collectible" (living) blocks — recovering
 * the block item, where a vanilla break gives nothing back. The pickup logic lives here (item-side,
 * so future items can implement it); the blocks stay pickup-unaware and only handle their own
 * contents-drop on removal. Removing the block runs its onRemove, so a pickup drops the block's
 * contents on the ground exactly like a break — the Forceps just additionally hand back the block.
 */
public interface ICollectBlocks {

    default boolean canCollect(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.COLLECTIBLE);
    }

    default void collect(Level level, BlockPos pos, Player player) {
        // Grab the block item while the block still exists.
        ItemStack blockItem = getBlockItem(level, pos);

        // A block entity marked IPreserveContentsOnPickup carries its contents with it: save its
        // data onto the recovered item and remove the block entity BEFORE clearing the block, so
        // the block's onRemove finds no block entity and spills nothing. Everything else clears
        // normally, letting onRemove drop the contents on the ground like a break.
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IPreserveContentsOnPickup) {
            be.saveToItem(blockItem, level.registryAccess());
            level.removeBlockEntity(pos);
        }

        changeToAir(level, pos);
        grabItem(player, blockItem);
        playDefaultPickupSound(level, pos);
    }

    default ItemStack getBlockItem(Level level, BlockPos pos) {
        return new ItemStack(level.getBlockState(pos).getBlock().asItem());
    }

    default void changeToAir(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    default void grabItem(Player player, ItemStack stack) {
        ModItemUtil.giveItem(player, stack);
    }

    default void playPickupSound(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    default void playDefaultPickupSound(Level level, BlockPos pos) {
        playPickupSound(level, pos, SoundEvents.SCULK_BLOCK_BREAK, 1f, 0.5f);
    }
}
