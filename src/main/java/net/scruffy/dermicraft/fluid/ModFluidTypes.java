package net.scruffy.dermicraft.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.scruffy.dermicraft.main.Dermicraft;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class ModFluidTypes {

    public static final ResourceLocation WATER_STILL_RL = ResourceLocation.parse("block/water_still");
    public static final ResourceLocation WATER_FLOWING_RL = ResourceLocation.parse("block/water_flow");
    public static final ResourceLocation WATER_OVERLAY_RL = ResourceLocation.parse("block/water_overlay");

    public static final ResourceLocation CHUNKY_FLUID_STILL_RL = ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "block/chunky_fluid_still");
    public static final ResourceLocation CHUNKY_FLUID_FLOWING_RL = ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "block/chunky_fluid_flow");

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Dermicraft.MOD_ID);

    //////////////////////////////Material Fluids\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FluidType> CALCIUM_BLEND_FLUID_TYPE = registerFluidType("calcium_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFFCE1E4,
                    setColorVector(239, 288, 227),
                    FluidType.Properties.create()
                            .viscosity(2500)
                            .density(2000)
                            .temperature(300)
                            .motionScale(0.010)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.LAVA_EXTINGUISH)
            ));

    public static final Supplier<FluidType> CARBON_BLEND_FLUID_TYPE = registerFluidType("carbon_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF343434,
                    setColorVector(52,52,52),
                    FluidType.Properties.create()
                            .viscosity(3000)
                            .density(2500)
                            .temperature(280)
                            .motionScale(0.012)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.LAVA_EXTINGUISH)
            ));

    public static final Supplier<FluidType> PROTEIN_BLEND_FLUID_TYPE = registerFluidType("protein_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF630606,
                    setColorVector(99, 6, 6),
                    FluidType.Properties.create()
                            .viscosity(2500)
                            .density(1200)
                            .temperature(310)
                            .motionScale(0.008)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(true)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    //////////////////////////////Catalysts\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FluidType> PRIMITIVE_CATALYST_FLUID_TYPE = registerFluidType("primitive_catalyst_fluid",
            new BaseFluidType(WATER_STILL_RL, WATER_FLOWING_RL, WATER_OVERLAY_RL, 0xFF5C4A30,
                    setColorVector(92,74, 48),
                    FluidType.Properties.create()
                            .viscosity(1500)
                            .density(1500)
                            .temperature(305)
                            .motionScale(0.025)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)));


    //////////////////////////////Slurries\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FluidType> CRUDE_SLURRY_FLUID_TYPE = registerFluidType("crude_slurry_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF4FA757,
                    setColorVector(252, 225, 228),
                    FluidType.Properties.create()
                            .viscosity(4000)
                            .density(3000)
                            .temperature(285)
                            .motionScale(0.08)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(true)
                            .canConvertToSource(false)
            ));


    //////////////////////////////Sediment Blends\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Placeholder numeric values (density/viscosity/motionScale) - exact balance not yet decided, see crafting notes.
    public static final Supplier<FluidType> STONE_BLEND_FLUID_TYPE = registerFluidType("stone_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF7D7D7D,
                    setColorVector(125, 125, 125),
                    FluidType.Properties.create()
                            .viscosity(3500)
                            .density(3500)
                            .temperature(290)
                            .motionScale(0.006)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> SILICA_BLEND_FLUID_TYPE = registerFluidType("silica_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFD9C18C,
                    setColorVector(217, 193, 140),
                    FluidType.Properties.create()
                            .viscosity(4000)
                            .density(3000)
                            .temperature(290)
                            .motionScale(0.003)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> CLAY_BLEND_FLUID_TYPE = registerFluidType("clay_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFA1A8AB,
                    setColorVector(161, 168, 171),
                    FluidType.Properties.create()
                            .viscosity(4500)
                            .density(2800)
                            .temperature(290)
                            .motionScale(0.005)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));


    //////////////////////////////Metal Blends\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Ferrous Blend - base metal: Iron
    public static final Supplier<FluidType> FERROUS_BLEND_FLUID_TYPE = registerFluidType("ferrous_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFD8D8D8,
                    setColorVector(216, 216, 216),
                    FluidType.Properties.create()
                            .viscosity(2000)
                            .density(2200)
                            .temperature(290)
                            .motionScale(0.015)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    // Cuprous Blend - base metal: Copper
    public static final Supplier<FluidType> CUPROUS_BLEND_FLUID_TYPE = registerFluidType("cuprous_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFB87333,
                    setColorVector(184, 115, 51),
                    FluidType.Properties.create()
                            .viscosity(2000)
                            .density(2200)
                            .temperature(290)
                            .motionScale(0.015)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    // Aurous Blend - base metal: Gold
    public static final Supplier<FluidType> AUROUS_BLEND_FLUID_TYPE = registerFluidType("aurous_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFD4AF37,
                    setColorVector(212, 175, 55),
                    FluidType.Properties.create()
                            .viscosity(2000)
                            .density(2200)
                            .temperature(290)
                            .motionScale(0.015)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));


    //////////////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    private static Vector3f setColorVector(float r, float g, float b) {
        return new Vector3f(r/255f, g/255f, b/255f);
    }

    private static Supplier<FluidType> registerFluidType(String name, FluidType fluidType) {
        return FLUID_TYPES.register(name, () -> fluidType);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}
