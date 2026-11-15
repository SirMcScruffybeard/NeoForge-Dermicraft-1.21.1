package net.scruffy.dermicraft.util;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A view of a fluid handler that can only be drained: reads and drains go through to the real handler, but nothing can be
 * poured in. For a machine face that is an output only (pipes pull from it, nothing pushes into it).
 */
public final class DrainOnlyFluidHandler implements IFluidHandler {

    private final IFluidHandler delegate;

    public DrainOnlyFluidHandler(IFluidHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return delegate.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return false; // nothing may be inserted
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return 0;
    }

    @NotNull
    @Override
    public FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
        return delegate.drain(resource, action);
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, @NotNull FluidAction action) {
        return delegate.drain(maxDrain, action);
    }
}
