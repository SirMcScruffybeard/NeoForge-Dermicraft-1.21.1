package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Accepts an injection from an {@link IInject} item -- mirrors {@link IHarvestableBlock}/
 * {@link ISutableBlock}'s shape. Exposed as its own interface (rather than staying a private
 * method on {@code MarredTumorBlock}) so a mode-driven item like A.I.D. can call it directly,
 * gated on its own current mode, the same reasoning those two interfaces already exist for.
 *
 * <p>{@code blockEntity} is the generic {@link BlockEntity}, not a specific implementor's own type
 * (e.g. {@code MarredTumorBlockEntity}/{@code CrawBlockEntity}) -- each implementation casts/
 * instanceof-checks internally, the same way a mismatched type is just "no injection" rather than
 * a caller-side branch per block. Lets {@code AidItem#useSyringe} dispatch to any implementor
 * uniformly instead of hardcoding one target block entity type.
 */
public interface IInjectableBlock {

    /** @return whether the injection actually took (a matching recipe/fluid was found) -- lets a
     * caller like A.I.D. know whether to play its own "took effect" feedback. */
    boolean inject(Level level, Player player, ItemStack stack, BlockEntity blockEntity);

    /**
     * Whether this block accepts an injection in the given state (default: always). Callers that
     * dispatch by interface (A.I.D., its target scanner) check this first so a block that is only
     * injectable in one stage, like an open Marred Tumor, is not treated as a target. An
     * implementor's {@link #inject} should still guard itself.
     */
    default boolean canInject(BlockState state) {
        return true;
    }

    default void playEvolveSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 1.2F, 0.5F);
    }
}
