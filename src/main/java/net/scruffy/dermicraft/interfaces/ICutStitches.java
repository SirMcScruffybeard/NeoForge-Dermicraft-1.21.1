package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.block.entity.custom.MarredTumorBlockEntity;

/**
 * Reverts a stitched Marred Tumor back to its open stage -- the mirror image of {@link ISutableBlock}.
 * Exposed as its own interface (rather than staying a private method on {@code MarredTumorBlock})
 * so a mode-driven item like A.I.D. can call it directly, gated on its own current mode, the same
 * reasoning {@link IHarvestableBlock}/{@link ISutableBlock} already exist for. Implementors should
 * guard on their own stage, since the block is the same one in both stages.
 */
public interface ICutStitches {

    void cutStitches(Level level, BlockPos pos, Player player, ItemStack scalpelStack, MarredTumorBlockEntity tumorEntity);

    default void playStitchCutSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 0.7F);
    }
}
