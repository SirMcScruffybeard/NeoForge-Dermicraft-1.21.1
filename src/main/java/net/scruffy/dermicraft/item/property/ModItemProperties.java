package net.scruffy.dermicraft.item.property;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModItemProperties {
    public static void addCustomItemProperties() {

        ItemProperties.register(ModItems.SYRINGE.get(),
                getResourceLocation("full"), (stack, level, entity, seed) -> {
                    FluidData data= stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
       if (data.getFluidType() == Fluids.LAVA.getFluidType()) return 3;
       if (data.getFluidStack().is(FluidTags.WATER)) return 1;
       if(data.getFluidStack().is(ModTags.Fluids.BIOFUELS)) return 2;
       if (!data.isFluidEmpty()) return 1;
       return 0;
        });
    }

    //////////////HelperMethods\\\\\\\\\\\\\\
    private static ResourceLocation getResourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, path);
    }

    private static DataComponentType<FluidData> getFluidDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

}
