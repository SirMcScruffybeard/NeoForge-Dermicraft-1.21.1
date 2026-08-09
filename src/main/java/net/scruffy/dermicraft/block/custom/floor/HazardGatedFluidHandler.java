package net.scruffy.dermicraft.block.custom.floor;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.hazard.HazardProfile;

/**
 * Wraps a real {@link IFluidHandler} so any fluid the network's weakest-link {@link HazardProfile}
 * doesn't tolerate is invisible and untouchable through this view -- the "unsupported fluid = hard
 * block" rule from dermicraft-machine-notes.md -> Lab Floor -> Path resolution, applied to the Gear
 * Stations' shared pool ({@link FloorNetwork#fluidHandlers}). Delegate contents may already hold a
 * disallowed fluid (e.g. a player poured lava into a connected tank directly) -- gating only
 * {@code fill} wouldn't hide that, so every read/write path here checks the profile, not just fills.
 */
final class HazardGatedFluidHandler implements IFluidHandler {

    private final IFluidHandler delegate;
    private final HazardProfile profile;

    HazardGatedFluidHandler(IFluidHandler delegate, HazardProfile profile) {
        this.delegate = delegate;
        this.profile = profile;
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        FluidStack stack = delegate.getFluidInTank(tank);
        return profile.accepts(stack) ? stack : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return profile.accepts(stack) && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!profile.accepts(resource)) return 0;
        return delegate.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!profile.accepts(resource)) return FluidStack.EMPTY;
        return delegate.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // A size-only request doesn't say which fluid it wants, so there's nothing to check against
        // up front -- simulate first, then refuse if what it would have drained fails the profile.
        FluidStack simulated = delegate.drain(maxDrain, FluidAction.SIMULATE);
        if (simulated.isEmpty() || !profile.accepts(simulated)) return FluidStack.EMPTY;
        return delegate.drain(maxDrain, action);
    }
}
