package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scruffy.dermicraft.datagen.tag.ModTags;

public class VulnerableTank extends FluidTank {

    public VulnerableTank(int capacity) {
        super(capacity, (fluidStack -> !fluidStack.is(ModTags.Fluids.HAZARDOUS)));
    }
}
