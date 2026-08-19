package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.custom.duct.AbstractInnardsDuctBlock;
import net.scruffy.dermicraft.block.custom.duct.AbstractNodeBlock;
import net.scruffy.dermicraft.block.custom.duct.DuctRunResolver;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.property.BiofuelProperties;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ModFluidUtil {

    public static boolean hasRoom(IFluidHandler tank, FluidStack resource) {
        if (resource.isEmpty()) return false;
        int accepted = tank.fill(resource, IFluidHandler.FluidAction.SIMULATE);
        return accepted >= resource.getAmount();
    }

    public static boolean hasRoom(IFluidHandler tank, int amount, int slot) {
        if (amount == 0) return false;
        return amount < getRoom(tank, slot);
    }

    public static int getRoom(IFluidHandler tank, int index) {
        return tank.getTankCapacity(index) - tank.getFluidInTank(index).getAmount();
    }

    public static boolean isFull(IFluidHandler tank, int index) {
        return getRoom(tank, index) <= 0;
    }

    //////////Slot Check & Internal Transfer Methods\\\\\\\\\\

    /********************************************************************************
     *  Checks if the item of the passed slot of the passed itemHandler is a valid
     * puddle handler and has puddle in it to be emptied
     * @param itemHandler
     * @param slot
     * @return
     ********************************************************************************/
    @Nullable
    private static IFluidHandlerItem getItemFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.getCapability(Capabilities.FluidHandler.ITEM, null);
    }

    public static boolean hasFluidHandlerInSlot(IItemHandler itemHandler, int slot) {
        IFluidHandlerItem handler = getItemFluidHandler(itemHandler.getStackInSlot(slot));
        return handler != null && !handler.getFluidInTank(0).isEmpty();
    }

    public static boolean hasEmptyFluidHandlerInSlotForTransfer(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        IFluidHandlerItem handler = getItemFluidHandler(itemHandler.getStackInSlot(slot));
        // isEmpty(), not != FluidStack.EMPTY: the latter is reference identity against the EMPTY
        // singleton, which only happened to work because tryFluidTransfer returns exactly that
        // instance on failure. Any path handing back a distinct zero-amount stack would have read
        // as a successful transfer.
        return handler != null
                && (handler.getFluidInTank(0).isEmpty()
                || !FluidUtil.tryFluidTransfer(handler, tank, Integer.MAX_VALUE, false).isEmpty());
    }

    public static boolean hasEmptyFluidHandlerInSlot(ItemStackHandler itemHandler, int slot) {
        IFluidHandlerItem handler = getItemFluidHandler(itemHandler.getStackInSlot(slot));
        return handler != null && handler.getFluidInTank(0).isEmpty();
    }

    public static void transferFluidToTank(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        FluidActionResult result = FluidUtil.tryEmptyContainer(itemHandler.getStackInSlot(slot), tank, Integer.MAX_VALUE, null, true);
        if (!result.result.isEmpty()) {
            itemHandler.setStackInSlot(slot, result.result);
        }
    }

    public static void transferFluidFromTankToHandler(ItemStackHandler itemHandler, int itemSlot, FluidTank tank) {
        FluidActionResult result = FluidUtil.tryFillContainer(itemHandler.getStackInSlot(itemSlot), tank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess() && !result.result.isEmpty()) {
            itemHandler.setStackInSlot(itemSlot, result.result);
        }
    }

    //////////Container fill/drain with container-swap write-back\\\\\\\\\\

    /*
     * WHY THESE EXIST -- read before hand-rolling another container fill.
     *
     * An IFluidHandlerItem is NOT required to mutate the ItemStack it was resolved from. Our own
     * component-backed containers (Bladder, Beaker, Flask, every gadget tank) do, because the stack
     * IS the handler's container -- so a fill/drain writes straight through and nothing else is
     * needed. Vanilla's bucket wrapper does not: it represents a fill as swapping to a whole
     * different Item (empty bucket <-> lava bucket) and reflects that ONLY on getContainer().
     *
     * Miss the write-back and the source is debited while the destination stack never changes --
     * the fluid is silently destroyed. That bug has now been found three separate times (D.R.I.N.K.E.R.
     * Transfer, I.D.E.P. auto-fill, and earlier the Bladder refuel path), always in code that fills
     * an ARBITRARY player-inventory container, which is the only place a vanilla bucket turns up.
     * Prefer these helpers over calling fill()/drain() plus a hand-written write-back.
     */

    /**
     * Fills the container in {@code slot} of {@code itemHandler} from {@code source}, writing any
     * swapped container back into that same slot.
     *
     * @return mB actually moved (0 if the container refused it, or the slot held no container)
     */
    public static int fillContainerInSlot(ItemStackHandler itemHandler, int slot, IFluidHandler source, int max) {
        ItemStack container = itemHandler.getStackInSlot(slot);
        IFluidHandlerItem handler = getItemFluidHandler(container);
        if (handler == null) return 0;

        int moved = transferInto(handler, source, max);
        if (moved > 0 && handler.getContainer() != container) {
            itemHandler.setStackInSlot(slot, handler.getContainer());
        }
        return moved;
    }

    /**
     * Fills {@code container} from {@code source}, writing any swapped container back into the
     * player's inventory (or handing it to them if the original wasn't found there).
     *
     * <p>Only ever call this with a single (unstacked) container -- a fluid data component belongs
     * to the WHOLE stack, so filling a stack of five in place fills all five from one container's
     * worth. See {@code IHaveFluidData#isSingleContainer}; split one off first if you need stacked
     * containers to work.
     *
     * @return mB actually moved (0 if the container refused it)
     */
    public static int fillPlayerContainer(net.minecraft.world.entity.player.Player player,
                                          ItemStack container, IFluidHandler source, int max) {
        IFluidHandlerItem handler = getItemFluidHandler(container);
        if (handler == null) return 0;

        int moved = transferInto(handler, source, max);
        if (moved > 0 && handler.getContainer() != container) {
            writeBackToPlayer(player, container, handler.getContainer());
        }
        return moved;
    }

    /**
     * Whether {@code destination} will take {@code fluid} at all, asked BEFORE offering it.
     *
     * <p>A successful fill() is not on its own proof the destination SHOULD have taken it: only some
     * containers gate on hazard (the Bladder family does, the Beaker/Glass Flask/I.D.E.P. and every
     * vanilla bucket do not), and a handler that doesn't gate will happily swallow a fluid its tier
     * forbids. {@link IFluidHandler#isFluidValid} is where a gated handler expresses that refusal.
     */
    public static boolean canHold(IFluidHandler destination, FluidStack fluid) {
        for (int tank = 0; tank < destination.getTanks(); tank++) {
            if (destination.isFluidValid(tank, fluid)) return true;
        }
        return false;
    }

    /** Drain-then-fill sized by what the destination will actually take, so nothing is ever pulled
     * out of {@code source} that {@code destination} can't accept. Gated by {@link #canHold}. */
    private static int transferInto(IFluidHandler destination, IFluidHandler source, int max) {
        FluidStack available = source.drain(max, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty() || !canHold(destination, available)) return 0;

        int accepted = destination.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return 0;

        // Drain by stack, not by amount: on a multi-tank source, drain(int) could pull a different
        // fluid than the one just approved.
        FluidStack request = available.copy();
        request.setAmount(accepted);
        FluidStack drained = source.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return 0;

        return destination.fill(drained, IFluidHandler.FluidAction.EXECUTE);
    }

    /** Replaces {@code original} wherever it sits in the player's inventory. getContainerSize()
     * spans main/armor/offhand, so a container the caller found on the player is always located;
     * the fallback only guards against losing it if it somehow isn't.
     *
     * <p>Public for callers doing their own fill loop rather than using {@link #fillPlayerContainer}
     * -- e.g. one tracking a running remainder across several containers. */
    public static void writeBackToPlayer(net.minecraft.world.entity.player.Player player,
                                         ItemStack original, ItemStack replacement) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == original) {
                inventory.setItem(slot, replacement);
                return;
            }
        }
        net.scruffy.dermicraft.interfaces.IHaveFluidData.giveOrDrop(player, replacement);
    }

    //////////External Transfer Methods\\\\\\\\\\
    public static void pushFluidToAboveNeighbour(Level level, BlockPos worldPosition, FluidTank tank) {
        pushFluidToNeighbour(level, worldPosition, tank, Direction.UP);
    }

    public static void pushFluidToBelowNeighbour(Level level, BlockPos worldPosition, FluidTank tank) {
        pushFluidToNeighbour(level, worldPosition, tank, Direction.DOWN);
    }

    public static void pushFluidToNeighbour(Level level, BlockPos worldPosition, FluidTank tank, Direction direction) {
        BlockPos neighborPos = worldPosition.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        // A Node is never a passive auto-push target: every hop into the duct network is supposed
        // to be a decision the Node itself makes (its own IN leg + Item/Fluid toggles), not something
        // any adjacent block can force by simply existing there. Without this check a machine set
        // directly against a Node would dump straight into its tank -- unlimited amount, ignoring
        // the leg's direction mode and Fluid toggle entirely -- bypassing the whole routing model.
        if (neighborState.getBlock() instanceof AbstractNodeBlock) return;

        // Machine-direct duct drain: a duct directly below has no capability of its own, so walk the
        // run to whatever's on the far end (bounded to the 3x3 footprint below) instead of the usual
        // direct capability lookup. Only DOWN is wired up for this -- pushFluidToAboveNeighbour has
        // no current callers, so there's no "3x3 above" case to support yet.
        if (direction == Direction.DOWN && neighborState.getBlock() instanceof AbstractInnardsDuctBlock) {
            pushFluidThroughDuct(level, worldPosition, tank);
            return;
        }

        Direction hitSide = direction.getOpposite();
        FluidUtil.getFluidHandler(level, neighborPos, hitSide).ifPresent(neighborHandler -> {
            FluidUtil.tryFluidTransfer(neighborHandler, tank, Integer.MAX_VALUE, true);
        });
    }

    private static void pushFluidThroughDuct(Level level, BlockPos worldPosition, FluidTank tank) {
        Optional<DuctRunResolver.Endpoint> endpointOpt =
                DuctRunResolver.resolveWithinFootprint(level, worldPosition, Direction.DOWN, DuctRunResolver.DRAIN_MAX_HOPS);
        if (endpointOpt.isEmpty()) return;
        DuctRunResolver.Endpoint endpoint = endpointOpt.get();

        // The run may end at a Node -- same rule as direct adjacency: never push into one, it must
        // pull deliberately via its own leg config.
        if (level.getBlockState(endpoint.pos()).getBlock() instanceof AbstractNodeBlock) return;

        FluidStack current = tank.getFluid();
        if (current.isEmpty() || !endpoint.hazardProfile().accepts(current)) return;

        FluidUtil.getFluidHandler(level, endpoint.pos(), endpoint.accessDirection()).ifPresent(neighborHandler ->
                FluidUtil.tryFluidTransfer(neighborHandler, tank, Integer.MAX_VALUE, true));
    }

    //////////Biofuel Checkers, Getters, Setters\\\\\\\\\\
    @Nullable
    private static BiofuelProperties getBiofuelData(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) return null;
        return BuiltInRegistries.FLUID.wrapAsHolder(fluidStack.getFluid()).getData(ModDataMaps.BIOFUELS);
    }

    public static boolean isBiofuel(FluidStack fluidStack) {
        return getBiofuelData(fluidStack) != null;
    }

    public static float getUseRate(FluidStack fluidStack) {
        BiofuelProperties data = getBiofuelData(fluidStack);
        return data == null ? 0 : data.useRate();
    }

    public static float getSpeed(FluidStack fluidStack) {
        BiofuelProperties data = getBiofuelData(fluidStack);
        return data == null ? 0 : data.speed();
    }

    public static float getHeal(FluidStack fluidStack) {
        BiofuelProperties data = getBiofuelData(fluidStack);
        return data == null ? 0 : data.heal();
    }

    public static int getTier(FluidStack fluidStack) {
        BiofuelProperties data = getBiofuelData(fluidStack);
        return data == null ? 0 : data.tier();
    }
}
