package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.common.Tags;

public class WaterTank extends ModFluidTank {

    public WaterTank(int capacity, int slot) {
        super(capacity, slot, (fluidStack -> fluidStack.is(Tags.Fluids.WATER)));
    }
}
