package net.scruffy.dermicraft.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Accepts an injection from an {@link IInject} item -- mirrors {@link IHarvestableBlock}/
 * {@link ISutableBlock}'s shape. Exposed as its own interface (rather than staying a private
 * method on {@code StitchedTumorBlock}) so a mode-driven item like A.I.D. can call it directly,
 * gated on its own current mode, the same reasoning those two interfaces already exist for.
 *
 * <p>{@code blockEntity} is the generic {@link BlockEntity}, not a specific implementor's own type
 * (e.g. {@code StitchedTumorBlockEntity}/{@code CrawBlockEntity}) -- each implementation casts/
 * instanceof-checks internally, the same way a mismatched type is just "no injection" rather than
 * a caller-side branch per block. Lets {@code AidItem#useSyringe} dispatch to any implementor
 * uniformly instead of hardcoding one target block entity type.
 */
public interface IInjectableBlock {

    /** @return whether the injection actually took (a matching recipe/fluid was found) -- lets a
     * caller like A.I.D. know whether to play its own "took effect" feedback. */
    boolean inject(Level level, Player player, ItemStack stack, BlockEntity blockEntity);
}
