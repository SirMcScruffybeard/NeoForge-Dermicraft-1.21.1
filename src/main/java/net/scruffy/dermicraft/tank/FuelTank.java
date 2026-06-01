package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.util.ModFluidUtil;

public class FuelTank extends ModFluidTank {

    public static final float BASE_SPEED_MODIFIER = 0.1f;

    public FuelTank(int capacity) {
        super(capacity, ModFluidUtil::isBiofuel);
    }

    //////////Biofuel Checkers, Getters, Setters\\\\\\\\\\
    public boolean isBiofuel() {
        return ModFluidUtil.isBiofuel(this.getFluid());
    }

    public int getUseRate() {
        if (this.isEmpty() || !isBiofuel()) return 0;
        return Math.round(ModFluidUtil.getUseRate(this.getFluid()));
    }

    public float getFuelSpeedModifier() {
        if (this.isEmpty() || !isBiofuel()) return 0;
        return ModFluidUtil.getSpeed(this.getFluid());
    }

    public float getSpeed() {
       return  getFuelSpeedModifier() / BASE_SPEED_MODIFIER;
    }

    public boolean hasEnoughFuel(int targetAmount) {
        return hasEnoughFluid(this, targetAmount);
    }

    public void useFuel(int amount) {
        if(hasEnoughFuel(amount)) drain(amount, FluidAction.EXECUTE);
    }

    public void internalFuelTransfer(ItemStackHandler inventory, int slot) {
        super.internalFluidTransfer(inventory, slot, this);
    }

}
