package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class WaterTank extends ModFluidTank {

    public WaterTank(int capacity, int slot) {
        super(capacity, slot, (fluidStack -> fluidStack.is(Tags.Fluids.WATER)));
    }
}
