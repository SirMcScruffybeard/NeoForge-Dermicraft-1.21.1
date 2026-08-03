package net.scruffy.dermicraft.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;

/**
 * Field tool for swapping Sunder's chain (and eventually Drill Hammer's head) without a Workbench
 * trip -- named after the real chainsaw tool's common nickname. Wears 1 point of durability per
 * completed chain swap (see {@code ScrenchMenu#applyCompletedSwapCosts}) -- a refuel-only session
 * costs nothing.
 *
 * <p>Cross-hand pairing check mirrors {@code SippingItem.tryHandTransfer}'s shape -- whichever hand
 * isn't holding the Scrench gets checked for Sunder. Covers "Scrench main hand, Sunder off hand"
 * only -- the reverse ordering is {@code SunderItem}'s own matching check, since Sunder's own
 * {@code use()} would otherwise always eat the click first. See {@link ScrenchMenu#open} for the
 * shared menu-opening logic both directions call.
 */
public class ScrenchItem extends Item {
    public ScrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!(player.getItemInHand(otherHand).getItem() instanceof SunderItem)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ScrenchMenu.open(serverPlayer, otherHand);
        }

        return InteractionResultHolder.consume(stack);
    }
}
