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

    public ModFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
    }

    public ModFluidTank(int capacity) {
        super(capacity);
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

    public boolean hasEnoughFluid(FluidTank tank, int targetAmount) {
        return tank.getFluid().getAmount() >= targetAmount;
    }

    public boolean hasFluidHandlerInSlot(IItemHandler itemHandler, int slot) {
        return ModFluidUtil.hasFluidHandlerInSlot(itemHandler,slot);
    }

    public boolean hasEmptyFluidHandlerInSlot(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        return ModFluidUtil.hasEmptyFluidHandlerInSlot(itemHandler, slot, tank);
    }

    public void transferFluidToTank(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        ModFluidUtil.transferFluidToTank(itemHandler, slot, tank);
    }

    public void transferResultFluidToTank(FluidStack resource) {
        if (hasRoom(resource)) {
            fill(resource, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public void internalFluidTransfer(ItemStackHandler inventory, int slot, FluidTank tank) {

        if (ModFluidUtil.hasFluidHandlerInSlot(inventory, slot)) {
            ModFluidUtil.transferFluidToTank(inventory, slot, tank);

        } else if (ModFluidUtil.hasEmptyFluidHandlerInSlot(inventory, slot, tank)) {
            ModFluidUtil.transferFluidFromTankToHandler(inventory, slot, tank);
        }
    }

    public void transferFluidFromTankToHandler(ItemStackHandler itemHandler, int itemSlot, FluidTank tank) {
        ModFluidUtil.transferFluidFromTankToHandler(itemHandler, itemSlot, tank);
    }

    public void  pushFluidToAboveNeighbour(Level level, BlockPos worldPosition){
        ModFluidUtil.pushFluidToAboveNeighbour(level, worldPosition, this);
    }

    public void  pushFluidToBelowNeighbour(Level level, BlockPos worldPosition){
        ModFluidUtil.pushFluidToBelowNeighbour(level, worldPosition, this);
    }


    
    
}
