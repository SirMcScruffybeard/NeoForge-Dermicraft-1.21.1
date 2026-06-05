package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public class WaterTank extends ModFluidTank {

    public WaterTank(int capacity, int slot) {
        super(capacity, slot, (fluidStack -> fluidStack.is(Tags.Fluids.WATER)));
    }

    public boolean hasEmptyFluidHandlerInSlot(ItemStackHandler inventory, int slot) {
        return super.hasEmptyFluidHandlerInSlot(inventory, slot, this);
    }

    public void transferFluidFromTankToHandler(ItemStackHandler inventory, int slot) {
        super.transferFluidFromTankToHandler(inventory, slot, this);
    }
}
