package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class WaterTank extends FluidTank {

    public WaterTank(int capacity) {
        super(capacity, (fluidStack -> fluidStack.is(Tags.Fluids.WATER)));
    }
}
