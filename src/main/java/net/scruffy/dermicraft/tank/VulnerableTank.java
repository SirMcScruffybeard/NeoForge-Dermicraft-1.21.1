package net.scruffy.dermicraft.tank;

import net.scruffy.dermicraft.datagen.tag.ModTags;

public class VulnerableTank extends ModFluidTank {

    public VulnerableTank(int capacity, int slot) {
        super(capacity, slot, (fluidStack -> !fluidStack.is(ModTags.Fluids.HAZARDOUS)));
    }
}
