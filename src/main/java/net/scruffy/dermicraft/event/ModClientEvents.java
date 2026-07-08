package net.scruffy.dermicraft.event;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.fluid.ModFluidTypes;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Supplier;

@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        registerFluidTint(event, ModItems.SYRINGE.get());
        registerFluidTint(event, ModItems.GLASS_FLASK.get());
        registerFluidTint(event, ModBlocks.BEAKER_ITEM.get());

        ////////////////////Buckets\\\\\\\\\\\\\\\\\\\\
        registerBucketTint(event, ModFluids.CALCIUM_BLEND_BUCKET.get(), ModFluidTypes.CALCIUM_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.CARBON_BLEND_BUCKET.get(), ModFluidTypes.CARBON_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.PROTEIN_BLEND_BUCKET.get(), ModFluidTypes.PROTEIN_BLEND_FLUID_TYPE);

        registerBucketTint(event, ModFluids.CRUDE_SLURRY_BUCKET.get(), ModFluidTypes.CRUDE_SLURRY_FLUID_TYPE);

        registerBucketTint(event, ModFluids.PRIMITIVE_CATALYST_BUCKET.get(), ModFluidTypes.PRIMITIVE_CATALYST_FLUID_TYPE);

        registerBucketTint(event, ModFluids.STONE_BLEND_BUCKET.get(), ModFluidTypes.STONE_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.SILICA_BLEND_BUCKET.get(), ModFluidTypes.SILICA_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.CLAY_BLEND_BUCKET.get(), ModFluidTypes.CLAY_BLEND_FLUID_TYPE);

        registerBucketTint(event, ModFluids.FERROUS_BLEND_BUCKET.get(), ModFluidTypes.FERROUS_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.CUPROUS_BLEND_BUCKET.get(), ModFluidTypes.CUPROUS_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.AUROUS_BLEND_BUCKET.get(), ModFluidTypes.AUROUS_BLEND_FLUID_TYPE);

        registerBucketTint(event, ModFluids.F_STUFF_BUCKET.get(), ModFluidTypes.F_STUFF_FLUID_TYPE);
        registerBucketTint(event, ModFluids.C_STUFF_BUCKET.get(), ModFluidTypes.C_STUFF_FLUID_TYPE);
    }

    //////////////HelperMethods\\\\\\\\\\\\\\
    private static void registerFluidTint(RegisterColorHandlersEvent.Item event, Item item) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return -1;

            FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
            if (data.isFluidEmpty()) return -1;

            Fluid fluid = data.getFluid();

            if (fluid.isSame(Fluids.LAVA)) return getLavaTint();

            // Fluid Type's color
            int color = IClientFluidTypeExtensions.of(fluid).getTintColor(data.fluidStack());

            //If the ingredientFluid returns "no color" (like -1), give it a default gray or water-blue
            return color == -1 ? getDefaultTint() : color;
        }, item);
    }

    // layer0 (bucket body) stays untinted; layer1 (fill overlay) is tinted with the fluid's color.
    private static void registerBucketTint(RegisterColorHandlersEvent.Item event, Item bucket, Supplier<FluidType> fluidType) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return -1;
            return IClientFluidTypeExtensions.of(fluidType.get()).getTintColor();
        }, bucket);
    }

    private static DataComponentType<FluidData> getFluidDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

    private static int getDefaultTint() {
        return 0xFFFFFFFF;
    }

    private static int getLavaTint(){
       // return 0xFFD45400; //Used with grayscale backing
        return getDefaultTint();
    }

}
