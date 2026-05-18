package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.component.DataComponentType;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;

public interface IInject {

    default DataComponentType<FluidData> getDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }
}
