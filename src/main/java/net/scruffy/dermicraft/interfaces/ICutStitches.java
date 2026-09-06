package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.block.entity.custom.StitchedTumorBlockEntity;

/**
 * Reverts a Stitched Tumor back to an open Marred Tumor -- the mirror image of {@link ISutableBlock}.
 * Exposed as its own interface (rather than staying a private method on {@code StitchedTumorBlock})
 * so a mode-driven item like A.I.D. can call it directly, gated on its own current mode, the same
 * reasoning {@link IHarvestableBlock}/{@link ISutableBlock} already exist for.
 */
public interface ICutStitches {

    void cutStitches(Level level, BlockPos pos, Player player, ItemStack scalpelStack, StitchedTumorBlockEntity stitchedEntity);
}
