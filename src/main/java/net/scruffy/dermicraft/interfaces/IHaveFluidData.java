package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;

public interface IHaveFluidData {

    default DataComponentType<FluidData> getDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

    default boolean isValidFluidHandler(IFluidHandler handler) {
        return handler != null;
    }

    default IFluidHandler getTargetFluidHandler(Level level, BlockPos pos, Direction face) {
        BlockEntity be = level.getBlockEntity(pos);
        IFluidHandler handler;
        if (be != null) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
            return handler;
        }
        return null;
    }

    default boolean targetHasEnough(int amount, IFluidHandler handler) {
        FluidStack fluidStack = handler.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        return fluidStack.getAmount() == amount;
    }

    default void emptyFluidData(ItemStack stack) {
        stack.set(getDataType(), FluidData.EMPTY);
    }

    default boolean isServerSide(Level level) {
        return !level.isClientSide;
    }

    private boolean hasFluid(FluidData data) {
        return data != null && !data.isFluidEmpty();
    }

    /**
     * General use Fluid Handler for use with FluidData data component.
     * Only fills/drains all-or-nothing, full capacity at a time.
     */
    class RigidFluidDataFluidHandler implements IFluidHandlerItem {

        protected ItemStack container;
        protected final int CAPACITY;

        public RigidFluidDataFluidHandler(ItemStack stack, int capacity) {
            container = stack;
            CAPACITY = capacity;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidData data = container.getOrDefault(getDataType(), FluidData.EMPTY);
            return data.getFluidStack();
        }

        @Override
        public int getTankCapacity(int i) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!getFluidInTank(0).isEmpty() || resource.isEmpty() || resource.getAmount() < getTankCapacity(0)) {
                return 0;
            }

            if (action.execute()) {
                container.set(getDataType(), FluidData.createData(resource.copy()));
            }
            return CAPACITY;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.is(getFluidInTank(0).getFluid())) {
                return drain(resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contained = getFluidInTank(0);
            if (contained.isEmpty() || maxDrain < CAPACITY) return FluidStack.EMPTY;

            if (action.execute()) {
                container.set(getDataType(), FluidData.EMPTY);
            }
            return contained;
        }

        private DataComponentType<FluidData> getDataType() {
            return ModDataComponentTypes.FLUID_DATA.get();
        }
    }

    /**
     * General use Fluid Handler for use with FluidData data component.
     * Allows partial fills/drains, up to the remaining room/amount available.
     */
    class FlexibleFluidDataFluidHandler implements IFluidHandlerItem {

        protected ItemStack container;
        protected final int CAPACITY;

        public FlexibleFluidDataFluidHandler(ItemStack stack, int capacity) {
            container = stack;
            CAPACITY = capacity;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidData data = container.getOrDefault(getDataType(), FluidData.EMPTY);
            return data.getFluidStack();
        }

        @Override
        public int getTankCapacity(int i) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;

            FluidStack contained = getFluidInTank(0);
            if (!contained.isEmpty() && !contained.is(resource.getFluid())) return 0;

            int space = CAPACITY - contained.getAmount();
            int amountToFill = Math.min(space, resource.getAmount());
            if (amountToFill <= 0) return 0;

            if (action.execute()) {
                FluidStack newStack = contained.isEmpty() ? resource.copy() : contained.copy();
                newStack.setAmount(contained.getAmount() + amountToFill);
                container.set(getDataType(), FluidData.createData(newStack));
            }
            return amountToFill;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.is(getFluidInTank(0).getFluid())) {
                return drain(resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contained = getFluidInTank(0);
            if (contained.isEmpty()) return FluidStack.EMPTY;

            int amountToDrain = Math.min(maxDrain, contained.getAmount());
            FluidStack drained = contained.copy();
            drained.setAmount(amountToDrain);

            if (action.execute()) {
                int remaining = contained.getAmount() - amountToDrain;
                if (remaining <= 0) {
                    container.set(getDataType(), FluidData.EMPTY);
                } else {
                    FluidStack remainingStack = contained.copy();
                    remainingStack.setAmount(remaining);
                    container.set(getDataType(), FluidData.createData(remainingStack));
                }
            }
            return drained;
        }

        private DataComponentType<FluidData> getDataType() {
            return ModDataComponentTypes.FLUID_DATA.get();
        }
    }
}
