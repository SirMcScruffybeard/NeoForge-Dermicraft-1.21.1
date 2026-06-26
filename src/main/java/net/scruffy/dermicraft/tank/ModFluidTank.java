package net.scruffy.dermicraft.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.util.ModFluidUtil;

import java.util.function.Predicate;

public abstract class ModFluidTank extends FluidTank {

    public static int BUCKET_VOLUME = FluidType.BUCKET_VOLUME;

    public final int SLOT;

    public ModFluidTank(int capacity, int slot, Predicate<FluidStack> validator) {
        super(capacity, validator);
        SLOT = slot;
    }

    public ModFluidTank(int capacity, int slot) {
        super(capacity);
        SLOT = slot;
    }

    public boolean hasRoom(FluidStack resource) {
        return getSpace() >= resource.getAmount();
    }

    public boolean hasRoom(int amount) {
        return getSpace() >= amount;
    }

    public boolean isFull() {
        return this.getSpace() <= 0;
    }

    public boolean hasEnoughFluid(int targetAmount) {
        return this.getFluid().getAmount() >= targetAmount;
    }

    public void useFluid(int amount) {
        this.drain(amount, FluidAction.EXECUTE);
    }

    public boolean hasFluidHandlerInSlot(IItemHandler itemHandler, int slot) {
        return ModFluidUtil.hasFluidHandlerInSlot(itemHandler, slot);
    }

    public boolean hasEmptyFluidHandlerInSlot(ItemStackHandler itemHandler, int slot) {
        return ModFluidUtil.hasEmptyFluidHandlerInSlotForTransfer(itemHandler, slot, this);
    }

    public void transferFluidToTank(ItemStackHandler itemHandler, int slot) {
        ModFluidUtil.transferFluidToTank(itemHandler, slot, this);
    }

    public void safeFill(FluidStack resource) {
        if (hasRoom(resource)) {
            fill(resource, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public void transferFluidFromTankToHandler(ItemStackHandler itemHandler, int itemSlot) {
        ModFluidUtil.transferFluidFromTankToHandler(itemHandler, itemSlot, this);
    }

    public void pushFluidToAboveNeighbour(Level level, BlockPos worldPosition) {
        ModFluidUtil.pushFluidToAboveNeighbour(level, worldPosition, this);
    }

    public void pushFluidToBelowNeighbour(Level level, BlockPos worldPosition) {
        ModFluidUtil.pushFluidToBelowNeighbour(level, worldPosition, this);
    }
}
