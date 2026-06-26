package net.scruffy.dermicraft.tank;

import net.scruffy.dermicraft.util.ModFluidUtil;

public class FuelTank extends ModFluidTank {

    public static final float BASE_SPEED_MODIFIER = 0.1f;

    public FuelTank(int capacity, int slot) {
        super(capacity, slot, ModFluidUtil::isBiofuel);
    }

    //////////Biofuel Checkers, Getters, Setters\\\\\\\\\\
    public boolean isBiofuel() {
        return ModFluidUtil.isBiofuel(this.getFluid());
    }

    private boolean isValidBiofuel() {
        return !this.isEmpty() && isBiofuel();
    }

    public int getUseRate() {
        if (!isValidBiofuel()) return 0;
        return Math.round(ModFluidUtil.getUseRate(this.getFluid()));
    }

    public float getFuelSpeedModifier() {
        if (!isValidBiofuel()) return 0;
        return ModFluidUtil.getSpeed(this.getFluid());
    }

    public float getSpeed() {
        return getFuelSpeedModifier() / BASE_SPEED_MODIFIER;
    }

    public boolean hasEnoughFuel(int targetAmount) {
        return hasEnoughFluid(targetAmount);
    }

    public void useFuel(int amount) {
        if (hasEnoughFuel(amount)) useFluid(amount);
    }
}
