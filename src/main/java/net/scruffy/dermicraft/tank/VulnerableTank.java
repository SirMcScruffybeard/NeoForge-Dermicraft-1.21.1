package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.datagen.tag.ModTags;

public class VulnerableTank extends ModFluidTank {

    public VulnerableTank(int capacity, int slot) {
        super(capacity, slot, (fluidStack -> !fluidStack.is(ModTags.Fluids.HAZARDOUS)));
    }

    public void transferToHandler(ItemStackHandler inventory, int slot) {
        if (hasEmptyFluidHandlerInSlot(inventory, slot, this)) {
            super.transferFluidFromTankToHandler(inventory, slot, this);
        }
    }

    public boolean hasEnoughFluid(int targetAmount) {
        return hasEnoughFluid(this, targetAmount);
    }


}
