package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;


public interface IInject {
    default DataComponentType<FluidData> getFluidDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

    default void emptyDataFluid(ItemStack stack) {
        stack.set(getFluidDataType(), FluidData.EMPTY);
    }
}
