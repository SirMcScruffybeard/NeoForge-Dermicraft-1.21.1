package net.scruffy.dermicraft.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;

/**
 * Field tool for swapping a gadget's own physical parts (Sunder's chain, Eater's Modules, and
 * eventually Drill Hammer's head) without a Workbench trip -- named after the real chainsaw tool's
 * common nickname. Generalized 2026-08-09 to check {@link IWorkbenchSwappable} rather than a
 * hardcoded {@code instanceof SunderItem} -- works with any gadget implementing that interface, no
 * changes needed here when a new one is added. Completed-swap costs (Sunder's movement penalty +
 * Scrench durability wear, Eater's recalibration delay, ...) are each gadget's own concern now, via
 * their {@code SwapPanel#onClosed}.
 *
 * <p>Cross-hand pairing check mirrors {@code SippingItem.tryHandTransfer}'s shape -- whichever hand
 * isn't holding the Scrench gets checked for a swappable gadget. Covers "Scrench main hand, gadget
 * off hand" only -- the reverse ordering is each gadget's own matching check (see {@code SunderItem}/
 * {@code EaterItem}'s own {@code use()}), since a gadget's own {@code use()} would otherwise always
 * eat the click first. See {@link ScrenchMenu#open} for the shared menu-opening logic both
 * directions call.
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
        if (!(player.getItemInHand(otherHand).getItem() instanceof IWorkbenchSwappable)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ScrenchMenu.open(serverPlayer, otherHand);
        }

        return InteractionResultHolder.consume(stack);
    }
}
