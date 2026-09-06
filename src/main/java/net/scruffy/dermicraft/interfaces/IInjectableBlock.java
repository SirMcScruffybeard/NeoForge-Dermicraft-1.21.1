package net.scruffy.dermicraft.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.block.entity.custom.StitchedTumorBlockEntity;

/**
 * Accepts an injection from an {@link IInject} item -- mirrors {@link IHarvestableBlock}/
 * {@link ISutableBlock}'s shape. Exposed as its own interface (rather than staying a private
 * method on {@code StitchedTumorBlock}) so a mode-driven item like A.I.D. can call it directly,
 * gated on its own current mode, the same reasoning those two interfaces already exist for.
 */
public interface IInjectableBlock {

    /** @return whether the injection actually took (a matching recipe/fluid was found) -- lets a
     * caller like A.I.D. know whether to play its own "took effect" feedback. */
    boolean inject(Level level, Player player, ItemStack stack, StitchedTumorBlockEntity blockEntity);
}
