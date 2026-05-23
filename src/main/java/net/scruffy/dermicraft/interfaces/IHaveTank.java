package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public interface IHaveTank {

    int BUCKET_VOLUME = FluidType.BUCKET_VOLUME;

    FluidStack getFluid();
    IFluidHandler getTank(@Nullable Direction face);

    default boolean hasRoom(IFluidHandler tank, FluidStack resource) {
        if (resource.isEmpty()) return false;
        int accepted = tank.fill(resource, IFluidHandler.FluidAction.SIMULATE);
        return accepted >= resource.getAmount();
    }

    default boolean isFull(IFluidHandler tank, int index) {
        return tank.getFluidInTank(index).getAmount() >= tank.getTankCapacity(index);
    }

    default boolean isEmpty(IFluidHandler tank, int index) {
        return tank.getFluidInTank(index).isEmpty();
    }

    //////////External Transfer Methods\\\\\\\\\\
    default void pushFluidToAboveNeighbour(Level level, BlockPos worldPosition, FluidTank tank) {
        pushFluidToNeighbour(level, worldPosition, tank, Direction.UP);
    }

    default void pushFluidToBelowNeighbour(Level level, BlockPos worldPosition, FluidTank tank) {
        pushFluidToNeighbour(level, worldPosition, tank, Direction.DOWN);
    }

    default void pushFluidToNeighbour(Level level, BlockPos worldPosition, FluidTank tank, Direction direction) {
        BlockPos neighborPos = worldPosition.relative(direction);
        Direction hitSide = direction.getOpposite();

        FluidUtil.getFluidHandler(level, neighborPos, hitSide).ifPresent(neighborHandler -> {
            FluidUtil.tryFluidTransfer(neighborHandler, tank, Integer.MAX_VALUE, true);
        });
    }

    //////////Slot Check & Internal Transfer Methods\\\\\\\\\\
    default boolean hasFluidHandlerInSlot(IItemHandler itemHandler, int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        return !stack.isEmpty()
                && stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null
                && !stack.getCapability(Capabilities.FluidHandler.ITEM, null).getFluidInTank(0).isEmpty();
    }

    default boolean hasEmptyFluidHandlerInSlot(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        return !stack.isEmpty()
                && stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null
                && (stack.getCapability(Capabilities.FluidHandler.ITEM, null).getFluidInTank(0).isEmpty() ||
                FluidUtil.tryFluidTransfer(stack.getCapability(Capabilities.FluidHandler.ITEM, null),
                        tank, Integer.MAX_VALUE, false) != FluidStack.EMPTY);
    }

    default void transferFluidToTank(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        FluidActionResult result = FluidUtil.tryEmptyContainer(itemHandler.getStackInSlot(slot), tank, Integer.MAX_VALUE, null, true);
        if(result.result != ItemStack.EMPTY) {
            itemHandler.setStackInSlot(slot, result.result);
        }
    }

    default void transferFluidFromTankToHandler(ItemStackHandler itemHandler, int itemSlot, FluidTank tank) {
        FluidActionResult result = FluidUtil.tryFillContainer(itemHandler.getStackInSlot(itemSlot), tank, Integer.MAX_VALUE, null, true);
        if (result.result != ItemStack.EMPTY && result.isSuccess()) {
            itemHandler.setStackInSlot(itemSlot, result.result);
        }
    }

    default void transferResultFluidToTank(IFluidHandler tank, FluidStack resource) {
        if (hasRoom(tank, resource)) {
            tank.fill(resource, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    //////////Spill\\\\\\\\\\


}
