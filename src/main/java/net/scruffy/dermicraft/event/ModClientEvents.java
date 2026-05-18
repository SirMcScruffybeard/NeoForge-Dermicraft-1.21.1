package net.scruffy.dermicraft.event;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {

        event.register((stack, tintIndex) -> {
            if(tintIndex != 0) return -1;

            FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
            if(data.isFluidEmpty()) return -1;

            Fluid fluid = data.getFluid();

            // 1. Manual Check for Lava
            if (fluid.isSame(Fluids.LAVA)) return getLavaTint();

            // 2. Fluid Type's color
            int color = IClientFluidTypeExtensions.of(fluid).getTintColor(data.fluidStack());

            // 3. If the fluid returns "no color" (like -1), give it a default gray or water-blue
            return color == -1 ? getDefaultTint() : color;
        }, ModItems.SYRINGE.get());

    }

    //////////////HelperMethods\\\\\\\\\\\\\\
    private static ResourceLocation getResourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, path);
    }

    private static DataComponentType<FluidData> getFluidDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

    private static int getDefaultTint() {
        return 0xFFFFFFFF;
    }

    private static int getLavaTint(){
        return 0xFFD45400;
    }

}
