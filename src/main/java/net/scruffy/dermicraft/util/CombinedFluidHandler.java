package net.scruffy.dermicraft.util;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Presents several {@link IFluidHandler}s as one, concatenated in list order -- the fluid-side
 * counterpart to NeoForge's {@code CombinedInvWrapper} (which only covers items). Used by an Innards
 * Gate Port that physically touches more than one Buffer, so a single duct line can feed/drain all
 * of them (e.g. one fuel line topping off two stacked machines' fuel Buffers).
 *
 * <p><b>fill = spillover:</b> fills the first handler, overflow continues into the next, and so on.
 * <b>drain = first-available:</b> pulls from handlers in order until the request is met, locking to
 * the first non-empty handler's fluid so a mixed set of Buffers never merges different fluids into
 * one drained stack. There is deliberately no awareness of channel IN/OUT intent here -- a Port is a
 * dumb block; mixing an input and an output Buffer on one Port is a documented player-side plumbing
 * error, not something this guards against.
 */
public class CombinedFluidHandler implements IFluidHandler {

    private final List<IFluidHandler> handlers;

    public CombinedFluidHandler(List<IFluidHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public int getTanks() {
        int total = 0;
        for (IFluidHandler handler : handlers) total += handler.getTanks();
        return total;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        for (IFluidHandler handler : handlers) {
            if (tank < handler.getTanks()) return handler.getFluidInTank(tank);
            tank -= handler.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        for (IFluidHandler handler : handlers) {
            if (tank < handler.getTanks()) return handler.getTankCapacity(tank);
            tank -= handler.getTanks();
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        for (IFluidHandler handler : handlers) {
            if (tank < handler.getTanks()) return handler.isFluidValid(tank, stack);
            tank -= handler.getTanks();
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        int remaining = resource.getAmount();
        int total = 0;
        for (IFluidHandler handler : handlers) {
            if (remaining <= 0) break;
            int filled = handler.fill(resource.copyWithAmount(remaining), action);
            total += filled;
            remaining -= filled;
        }
        return total;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        FluidStack result = FluidStack.EMPTY;
        int remaining = resource.getAmount();
        for (IFluidHandler handler : handlers) {
            if (remaining <= 0) break;
            FluidStack drained = handler.drain(resource.copyWithAmount(remaining), action);
            if (drained.isEmpty()) continue;
            if (result.isEmpty()) result = drained.copy();
            else result.grow(drained.getAmount());
            remaining -= drained.getAmount();
        }
        return result;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack result = FluidStack.EMPTY;
        int remaining = maxDrain;
        for (IFluidHandler handler : handlers) {
            if (remaining <= 0) break;
            // Once locked to a fluid, only keep draining that same fluid across the remaining
            // handlers -- never merge two different fluids into one drained stack.
            FluidStack drained = result.isEmpty()
                    ? handler.drain(remaining, action)
                    : handler.drain(result.copyWithAmount(remaining), action);
            if (drained.isEmpty()) continue;
            if (result.isEmpty()) result = drained.copy();
            else result.grow(drained.getAmount());
            remaining -= drained.getAmount();
        }
        return result;
    }
}
