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
        registerFluidTint(event, ModItems.BLADDER.get());
        registerFluidTint(event, ModItems.FUEL_BLADDER.get());
        registerFluidTint(event, ModItems.FEEDER_BLADDER.get());

        ////////////////////Buckets\\\\\\\\\\\\\\\\\\\\
        registerBucketTint(event, ModFluids.CALCIUM_BLEND_BUCKET.get(), ModFluidTypes.CALCIUM_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.CARBON_BLEND_BUCKET.get(), ModFluidTypes.CARBON_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.PROTEIN_BLEND_BUCKET.get(), ModFluidTypes.PROTEIN_BLEND_FLUID_TYPE);

        registerBucketTint(event, ModFluids.CRUDE_SLURRY_BUCKET.get(), ModFluidTypes.CRUDE_SLURRY_FLUID_TYPE);
        registerBucketTint(event, ModFluids.CONCENTRATED_SLURRY_BUCKET.get(), ModFluidTypes.CONCENTRATED_SLURRY_FLUID_TYPE);

        registerBucketTint(event, ModFluids.PRIMITIVE_CATALYST_BUCKET.get(), ModFluidTypes.PRIMITIVE_CATALYST_FLUID_TYPE);
        registerBucketTint(event, ModFluids.REINFORCING_CATALYST_BUCKET.get(), ModFluidTypes.REINFORCING_CATALYST_FLUID_TYPE);
        registerBucketTint(event, ModFluids.SYNAPSE_CATALYST_BUCKET.get(), ModFluidTypes.SYNAPSE_CATALYST_FLUID_TYPE);
        registerBucketTint(event, ModFluids.EVOLUTION_CATALYST_BUCKET.get(), ModFluidTypes.EVOLUTION_CATALYST_FLUID_TYPE);

        registerBucketTint(event, ModFluids.STONE_BLEND_BUCKET.get(), ModFluidTypes.STONE_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.SILICA_BLEND_BUCKET.get(), ModFluidTypes.SILICA_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.CLAY_BLEND_BUCKET.get(), ModFluidTypes.CLAY_BLEND_FLUID_TYPE);

        registerBucketTint(event, ModFluids.FERROUS_BLEND_BUCKET.get(), ModFluidTypes.FERROUS_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.CUPROUS_BLEND_BUCKET.get(), ModFluidTypes.CUPROUS_BLEND_FLUID_TYPE);
        registerBucketTint(event, ModFluids.AUROUS_BLEND_BUCKET.get(), ModFluidTypes.AUROUS_BLEND_FLUID_TYPE);

        registerBucketTint(event, ModFluids.PULP_BLEND_BUCKET.get(), ModFluidTypes.PULP_BLEND_FLUID_TYPE);

        registerBucketTint(event, ModFluids.F_STUFF_BUCKET.get(), ModFluidTypes.F_STUFF_FLUID_TYPE);
        registerBucketTint(event, ModFluids.C_STUFF_BUCKET.get(), ModFluidTypes.C_STUFF_FLUID_TYPE);

        registerBucketTint(event, ModFluids.MOLTEN_REDSTONE_BUCKET.get(), ModFluidTypes.MOLTEN_REDSTONE_FLUID_TYPE);
        registerBucketTint(event, ModFluids.MOLTEN_QUARTZ_BUCKET.get(), ModFluidTypes.MOLTEN_QUARTZ_FLUID_TYPE);
        registerBucketTint(event, ModFluids.MOLTEN_GLOWSTONE_BUCKET.get(), ModFluidTypes.MOLTEN_GLOWSTONE_FLUID_TYPE);
        registerBucketTint(event, ModFluids.MOLTEN_AMETHYST_BUCKET.get(), ModFluidTypes.MOLTEN_AMETHYST_FLUID_TYPE);
        registerBucketTint(event, ModFluids.MOLTEN_DIAMOND_BUCKET.get(), ModFluidTypes.MOLTEN_DIAMOND_FLUID_TYPE);        registerBucketTint(event, ModFluids.MOLTEN_LAPIS_BUCKET.get(), ModFluidTypes.MOLTEN_LAPIS_FLUID_TYPE);
        registerBucketTint(event, ModFluids.MOLTEN_RAW_NETHERITE_SCRAP_BUCKET.get(), ModFluidTypes.MOLTEN_RAW_NETHERITE_SCRAP_FLUID_TYPE);
        registerBucketTint(event, ModFluids.MOLTEN_NETHERITE_BUCKET.get(), ModFluidTypes.MOLTEN_NETHERITE_FLUID_TYPE);
        registerBucketTint(event, ModFluids.BLAZE_ESSENCE_BUCKET.get(), ModFluidTypes.BLAZE_ESSENCE_FLUID_TYPE);
        registerBucketTint(event, ModFluids.GHAST_ESSENCE_BUCKET.get(), ModFluidTypes.GHAST_ESSENCE_FLUID_TYPE);
        registerBucketTint(event, ModFluids.WITHER_ESSENCE_BUCKET.get(), ModFluidTypes.WITHER_ESSENCE_FLUID_TYPE);
        registerBucketTint(event, ModFluids.ENDER_ESSENCE_BUCKET.get(), ModFluidTypes.ENDER_ESSENCE_FLUID_TYPE);
        registerBucketTint(event, ModFluids.MOLTEN_SOUL_SILICA_BUCKET.get(), ModFluidTypes.MOLTEN_SOUL_SILICA_FLUID_TYPE);

        ////////////////////Shatter Heads\\\\\\\\\\\\\\\\\\\\
        registerShatterHeadTint(event, ModItems.IRON_SHATTER_HEAD.get());
        registerShatterHeadTint(event, ModItems.GOLD_SHATTER_HEAD.get());
        registerShatterHeadTint(event, ModItems.COPPER_SHATTER_HEAD.get());
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

    // layer0 (face/tail) is tinted per-material via ModDataMaps.SHATTER_HEAD_PROPERTIES; layer1
    // (piston) stays untinted -- no handler registered for index 1, same "only register the index
    // that needs tinting" shape registerBucketTint uses.
    private static void registerShatterHeadTint(RegisterColorHandlersEvent.Item event, Item head) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return -1;

            net.scruffy.dermicraft.property.ShatterHeadProperties properties =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem())
                            .getData(net.scruffy.dermicraft.datagen.datamaps.ModDataMaps.SHATTER_HEAD_PROPERTIES);
            return properties == null ? getDefaultTint() : (0xFF000000 | properties.tint().getValue());
        }, head);
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
