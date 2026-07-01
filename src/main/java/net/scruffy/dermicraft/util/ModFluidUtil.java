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
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.property.BiofuelProperties;
import org.jetbrains.annotations.Nullable;

public class ModFluidUtil {

    public static boolean hasRoom(IFluidHandler tank, FluidStack resource) {
        if (resource.isEmpty()) return false;
        int accepted = tank.fill(resource, IFluidHandler.FluidAction.SIMULATE);
        return accepted >= resource.getAmount();
    }

    public static boolean hasRoom(IFluidHandler tank, int amount, int slot) {
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

    /********************************************************************************
     *  Checks if the item of the passed slot of the passed itemHandler is a valid
     * puddle handler and has puddle in it to be emptied
     * @param itemHandler
     * @param slot
     * @return
     ********************************************************************************/
    @Nullable
    private static IFluidHandlerItem getItemFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.getCapability(Capabilities.FluidHandler.ITEM, null);
    }

    public static boolean hasFluidHandlerInSlot(IItemHandler itemHandler, int slot) {
        IFluidHandlerItem handler = getItemFluidHandler(itemHandler.getStackInSlot(slot));
        return handler != null && !handler.getFluidInTank(0).isEmpty();
    }

    public static boolean hasEmptyFluidHandlerInSlotForTransfer(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        IFluidHandlerItem handler = getItemFluidHandler(itemHandler.getStackInSlot(slot));
        return handler != null
                && (handler.getFluidInTank(0).isEmpty()
                || FluidUtil.tryFluidTransfer(handler, tank, Integer.MAX_VALUE, false) != FluidStack.EMPTY);
    }

    public static boolean hasEmptyFluidHandlerInSlot(ItemStackHandler itemHandler, int slot) {
        IFluidHandlerItem handler = getItemFluidHandler(itemHandler.getStackInSlot(slot));
        return handler != null && handler.getFluidInTank(0).isEmpty();
    }

    public static void transferFluidToTank(ItemStackHandler itemHandler, int slot, FluidTank tank) {
        FluidActionResult result = FluidUtil.tryEmptyContainer(itemHandler.getStackInSlot(slot), tank, Integer.MAX_VALUE, null, true);
        if (!result.result.isEmpty()) {
            itemHandler.setStackInSlot(slot, result.result);
        }
    }

    public static void transferFluidFromTankToHandler(ItemStackHandler itemHandler, int itemSlot, FluidTank tank) {
        FluidActionResult result = FluidUtil.tryFillContainer(itemHandler.getStackInSlot(itemSlot), tank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess() && !result.result.isEmpty()) {
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
    @Nullable
    private static BiofuelProperties getBiofuelData(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) return null;
        return BuiltInRegistries.FLUID.wrapAsHolder(fluidStack.getFluid()).getData(ModDataMaps.BIOFUELS);
    }

    public static boolean isBiofuel(FluidStack fluidStack) {
        return getBiofuelData(fluidStack) != null;
    }

    public static float getUseRate(FluidStack fluidStack) {
        BiofuelProperties data = getBiofuelData(fluidStack);
        return data == null ? 0 : data.useRate();
    }

    public static float getSpeed(FluidStack fluidStack) {
        BiofuelProperties data = getBiofuelData(fluidStack);
        return data == null ? 0 : data.speed();
    }

    public static float getHeal(FluidStack fluidStack) {
        BiofuelProperties data = getBiofuelData(fluidStack);
        return data == null ? 0 : data.heal();
    }
}
