package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;



public interface IInject {

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


}
