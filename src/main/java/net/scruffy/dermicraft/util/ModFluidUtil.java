package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;

public class ModFluidUtil {

    public static boolean hasRoom(IFluidHandler tank, FluidStack resource) {
        if (resource.isEmpty()) return false;
        int accepted = tank.fill(resource, IFluidHandler.FluidAction.SIMULATE);
        return accepted >= resource.getAmount();
    }

    public static boolean hasRoom (IFluidHandler tank, int amount, int slot) {
        if (amount == 0) return false;
        return amount < getRoom(tank, slot);
    }

    public static int getRoom(IFluidHandler tank, int index) {
        return tank.getTankCapacity(index) - tank.getFluidInTank(index).getAmount();
    }

    public static boolean isFull(IFluidHandler tank, int index) {
        return getRoom(tank, index) <= 0;
    }

    //////////Slot Check & Internal Transfer Methods\\\\\\\\\\
    public static boolean hasFluidHandlerInSlot(IItemHandler itemHandler, int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        return !stack.isEmpty()
                && stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null
                && !stack.getCapability(Capabilities.FluidHandler.ITEM, null).getFluidInTank(0).isEmpty();
    }

    public static boolean hasEmptyFluidHandlerInSlot(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        return !stack.isEmpty()
                && stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null
                && (stack.getCapability(Capabilities.FluidHandler.ITEM, null).getFluidInTank(0).isEmpty() ||
                net.neoforged.neoforge.fluids.FluidUtil.tryFluidTransfer(stack.getCapability(Capabilities.FluidHandler.ITEM, null),
                        tank, Integer.MAX_VALUE, false) != FluidStack.EMPTY);
    }

    public static void transferFluidToTank(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        FluidActionResult result = net.neoforged.neoforge.fluids.FluidUtil.tryEmptyContainer(itemHandler.getStackInSlot(slot), tank, Integer.MAX_VALUE, null, true);
        if(result.result != ItemStack.EMPTY) {
            itemHandler.setStackInSlot(slot, result.result);
        }
    }

    public static void transferFluidFromTankToHandler(ItemStackHandler itemHandler, int itemSlot, FluidTank tank) {
        FluidActionResult result = net.neoforged.neoforge.fluids.FluidUtil.tryFillContainer(itemHandler.getStackInSlot(itemSlot), tank, Integer.MAX_VALUE, null, true);
        if (result.result != ItemStack.EMPTY && result.isSuccess()) {
            itemHandler.setStackInSlot(itemSlot, result.result);
        }
    }

    //////////External Transfer Methods\\\\\\\\\\
    public static void pushFluidToAboveNeighbour(Level level, BlockPos worldPosition, FluidTank tank) {
        pushFluidToNeighbour(level, worldPosition, tank, Direction.UP);
    }

    public static void pushFluidToBelowNeighbour(Level level, BlockPos worldPosition, FluidTank tank) {
        pushFluidToNeighbour(level, worldPosition, tank, Direction.DOWN);
    }

    public static void pushFluidToNeighbour(Level level, BlockPos worldPosition, FluidTank tank, Direction direction) {
        BlockPos neighborPos = worldPosition.relative(direction);
        Direction hitSide = direction.getOpposite();

        FluidUtil.getFluidHandler(level, neighborPos, hitSide).ifPresent(neighborHandler -> {
            FluidUtil.tryFluidTransfer(neighborHandler, tank, Integer.MAX_VALUE, true);
        });
    }

    //////////Biofuel Checkers, Getters, Setters\\\\\\\\\\
    public static boolean isBiofuel(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) return false;

        var holder = BuiltInRegistries.FLUID.wrapAsHolder(fluidStack.getFluid());

        return holder.getData(ModDataMaps.BIOFUELS) != null;
    }

    public static float getUseRate(FluidStack fluidStack) {
        if (!isBiofuel(fluidStack)) return 0;

        return BuiltInRegistries.FLUID.wrapAsHolder(fluidStack.getFluid()).getData(ModDataMaps.BIOFUELS).useRate();
    }

    public static float getSpeed(FluidStack fluidStack) {
        if (!isBiofuel(fluidStack)) return 0;

        return BuiltInRegistries.FLUID.wrapAsHolder(fluidStack.getFluid()).getData(ModDataMaps.BIOFUELS).speed();
    }
}
