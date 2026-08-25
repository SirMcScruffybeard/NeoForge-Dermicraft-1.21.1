package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.item.custom.SyringeItem;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Syringe-on-tank draw/inject, fired from {@link PlayerInteractEvent.RightClickBlock} rather than
 * {@code SyringeItem#useOn} -- that's still where the logic used to run, but every machine block's
 * {@code useWithoutItem} now unconditionally opens its GUI on a fallen-through click (see each
 * block's own "Opens the GUI directly..." comment), which consumes the interaction before vanilla
 * ever reaches the item's own {@code useOn} (see {@code ServerPlayerGameMode#useItemOn}: block's
 * {@code useItemOn} -> block's {@code useWithoutItem} -> ONLY THEN the item's {@code useOn}).
 * {@code RightClickBlock} fires earlier than all of that, so handling it here lets the Syringe win
 * regardless of what any given block's own interaction shape does, without touching every machine
 * block individually.
 *
 * <p>Draw and inject are symmetric: an empty Syringe pulls a full {@link SyringeItem#CAPACITY} out
 * of the target tank if it holds that much; a loaded Syringe pushes its held amount back in if the
 * tank has room for all of it. Either direction is all-or-nothing, matching the Syringe's fixed
 * 100 mB physical volume. Injection wears the tool (see {@code emptyDataFluidIfSurvival}); drawing
 * doesn't, matching {@code SyringeItem}'s own existing convention.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class SyringeTankEvent {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SyringeItem syringe)) return;

        BlockPos pos = event.getPos();
        Direction face = event.getFace();
        IFluidHandler handler = syringe.getTargetFluidHandler(level, pos, face);
        if (!syringe.isValidFluidHandler(handler)) return;

        Player player = event.getEntity();
        FluidData data = stack.getOrDefault(syringe.getFluidDataType(), FluidData.EMPTY);

        boolean handled = data.isFluidEmpty()
                ? tryDraw(handler, stack, syringe)
                : tryInject(handler, stack, syringe, player, data);
        if (!handled) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean tryDraw(IFluidHandler handler, ItemStack stack, SyringeItem syringe) {
        if (!syringe.targetHasEnough(SyringeItem.CAPACITY, handler)) return false;

        FluidStack drained = handler.drain(SyringeItem.CAPACITY, IFluidHandler.FluidAction.EXECUTE);
        stack.set(syringe.getFluidDataType(), FluidData.createData(drained));
        return true;
    }

    private static boolean tryInject(IFluidHandler handler, ItemStack stack, SyringeItem syringe,
                                      Player player, FluidData data) {
        FluidStack held = data.getFluidStack();
        int accepted = handler.fill(held, IFluidHandler.FluidAction.SIMULATE);
        if (accepted < held.getAmount()) return false;

        handler.fill(held, IFluidHandler.FluidAction.EXECUTE);
        syringe.emptyDataFluidIfSurvival(stack, player);
        return true;
    }
}
