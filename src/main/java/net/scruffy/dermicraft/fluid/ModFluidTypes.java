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

    //////////////////////////////Organic Fluids\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FluidType> CALCIUM_BLEND_FLUID_TYPE = registerFluidType("calcium_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFFCE1E4,
                    setColorVector(239, 288, 227),
                    FluidType.Properties.create()
                            .viscosity(1500)
                            .density(1300)
                            .temperature(300)
                            .motionScale(0.08)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.LAVA_EXTINGUISH)
            ));

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
