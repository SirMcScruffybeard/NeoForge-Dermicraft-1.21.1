package net.scruffy.dermicraft.tank;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
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

    public int getUseRate() {
        if (this.isEmpty() || !isBiofuel()) return 0;
        return Math.round(ModFluidUtil.getUseRate(this.getFluid()));
    }

    public float getFuelSpeedModifier() {
        if (this.isEmpty() || !isBiofuel()) return 0;
        return ModFluidUtil.getSpeed(this.getFluid());
    }

    public float getSpeed() {
        return getFuelSpeedModifier() / BASE_SPEED_MODIFIER;
    }

    public boolean hasEnoughFuel(int targetAmount) {
        return hasEnoughFluid(this, targetAmount);
    }

    public void useFuel(int amount) {
        if (hasEnoughFuel(amount)) useFluid(amount);
    }

    public boolean hasEmptyFluidHandlerInSlot(ItemStackHandler inventory, int fuelSlot) {
        return super.hasEmptyFluidHandlerInSlot(inventory, fuelSlot, this);
    }

    public void transferFuelToTankWithCheck(ItemStackHandler inventory, int fuelSlot) {
        if (super.hasFluidHandlerInSlot(inventory, fuelSlot))
            super.transferFluidToTank(inventory, fuelSlot, this);
    }

    public void transferFuelToTank(ItemStackHandler inventory, int fuelSlot) {
        super.transferFluidToTank(inventory, fuelSlot, this);
    }

    public void transferFuelToHandlerWithCheck(ItemStackHandler inventory, int fuelSlot) {
        if (hasEmptyFluidHandlerInSlot(inventory, fuelSlot))
            super.transferFluidFromTankToHandler(inventory, fuelSlot, this);
    }

    public void transferFuelToHandler(ItemStackHandler inventory, int fuelSlot) {
        super.transferFluidFromTankToHandler(inventory, fuelSlot, this);
    }

}
