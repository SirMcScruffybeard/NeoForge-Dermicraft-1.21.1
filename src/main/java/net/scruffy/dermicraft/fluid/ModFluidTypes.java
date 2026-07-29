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
                            .motionScale(0.0100)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(true)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> CONCENTRATED_SLURRY_FLUID_TYPE = registerFluidType("concentrated_slurry_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF3D8A47,
                    setColorVector(215, 235, 210),
                    FluidType.Properties.create()
                            .viscosity(4300)
                            .density(3200)
                            .temperature(288)
                            .motionScale(0.0090)
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


    //////////////////////////////Effluentcer Outputs\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // F-Stuff - Crude Slurry + Protein Blend, also directly cookable into MRE
    public static final Supplier<FluidType> F_STUFF_FLUID_TYPE = registerFluidType("f_stuff_fluid",
            new BaseFluidType(WATER_STILL_RL, WATER_FLOWING_RL, WATER_OVERLAY_RL, 0xFF8B5A2B,
                    setColorVector(139, 90, 43),
                    FluidType.Properties.create()
                            .viscosity(3250)
                            .density(2100)
                            .temperature(297)
                            .motionScale(0.02)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    // C-Stuff - Carbon Blend + Calcium Blend, also used in a standalone Syringe on Creepers
    public static final Supplier<FluidType> C_STUFF_FLUID_TYPE = registerFluidType("c_stuff_fluid",
            new BaseFluidType(WATER_STILL_RL, WATER_FLOWING_RL, WATER_OVERLAY_RL, 0xFF8C7B8A,
                    setColorVector(140, 123, 138),
                    FluidType.Properties.create()
                            .viscosity(2750)
                            .density(2250)
                            .temperature(290)
                            .motionScale(0.011)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));


    //////////////////////////////Pulp Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FluidType> PULP_BLEND_FLUID_TYPE = registerFluidType("pulp_blend_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFC29D62,
                    setColorVector(194, 157, 98),
                    FluidType.Properties.create()
                            .viscosity(3000)
                            .density(1400)
                            .temperature(290)
                            .motionScale(0.008)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    //////////////////////////////Molten Family (Stage 2)\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FluidType> MOLTEN_REDSTONE_FLUID_TYPE = registerFluidType("molten_redstone_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFB22222,
                    setColorVector(178, 40, 20),
                    FluidType.Properties.create()
                            .viscosity(5200)
                            .density(3200)
                            .temperature(1450)
                            .motionScale(0.035)
                            .lightLevel(8)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_QUARTZ_FLUID_TYPE = registerFluidType("molten_quartz_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFE3D6B5,
                    setColorVector(227, 214, 181),
                    FluidType.Properties.create()
                            .viscosity(5000)
                            .density(3200)
                            .temperature(1300)
                            .motionScale(0.007)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_GLOWSTONE_FLUID_TYPE = registerFluidType("molten_glowstone_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFFC9A857,
                    setColorVector(201, 168, 87),
                    FluidType.Properties.create()
                            .viscosity(5000)
                            .density(3200)
                            .temperature(1300)
                            .motionScale(0.007)
                            .lightLevel(10)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_AMETHYST_FLUID_TYPE = registerFluidType("molten_amethyst_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF8A5AA8,
                    setColorVector(138, 90, 168),
                    FluidType.Properties.create()
                            .viscosity(5000)
                            .density(3200)
                            .temperature(1300)
                            .motionScale(0.004)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_DIAMOND_FLUID_TYPE = registerFluidType("molten_diamond_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF6FCFC4,
                    setColorVector(111, 207, 196),
                    FluidType.Properties.create()
                            .viscosity(6200)
                            .density(3800)
                            .temperature(1300)
                            .motionScale(0.005)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_OBSIDIAN_FLUID_TYPE = registerFluidType("molten_obsidian_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF3D1A2C,
                    setColorVector(61, 26, 44),
                    FluidType.Properties.create()
                            .viscosity(6000)
                            .density(3000)
                            .temperature(1300)
                            .motionScale(0.007)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_LAPIS_FLUID_TYPE = registerFluidType("molten_lapis_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF1B3F8B,
                    setColorVector(27, 63, 139),
                    FluidType.Properties.create()
                            .viscosity(5000)
                            .density(3200)
                            .temperature(900)
                            .motionScale(0.007)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_RAW_NETHERITE_SCRAP_FLUID_TYPE = registerFluidType("molten_raw_netherite_scrap_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF3B2E28,
                    setColorVector(59, 46, 40),
                    FluidType.Properties.create()
                            .viscosity(5000)
                            .density(3200)
                            .temperature(500)
                            .motionScale(0.007)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_NETHERITE_FLUID_TYPE = registerFluidType("molten_netherite_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF5A4A32,
                    setColorVector(90, 74, 50),
                    FluidType.Properties.create()
                            .viscosity(6500)
                            .density(4000)
                            .temperature(600)
                            .motionScale(0.003)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> BLAZE_ESSENCE_FLUID_TYPE = registerFluidType("blaze_essence_fluid",
            new BaseFluidType(WATER_STILL_RL, WATER_FLOWING_RL, WATER_OVERLAY_RL, 0xFFFFB13D,
                    setColorVector(255, 177, 61),
                    FluidType.Properties.create()
                            .viscosity(5000)
                            .density(-1400)
                            .temperature(1600)
                            .motionScale(0.007)
                            .lightLevel(9)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> GHAST_ESSENCE_FLUID_TYPE = registerFluidType("ghast_essence_fluid",
            new BaseFluidType(WATER_STILL_RL, WATER_FLOWING_RL, WATER_OVERLAY_RL, 0xFFEDEDE3,
                    setColorVector(237, 237, 227),
                    FluidType.Properties.create()
                            .viscosity(4500)
                            .density(-800)
                            .temperature(900)
                            .motionScale(0.004)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> WITHER_ESSENCE_FLUID_TYPE = registerFluidType("wither_essence_fluid",
            new BaseFluidType(WATER_STILL_RL, WATER_FLOWING_RL, WATER_OVERLAY_RL, 0xFF262A22,
                    setColorVector(38, 42, 34),
                    FluidType.Properties.create()
                            .viscosity(5500)
                            .density(3400)
                            .temperature(400)
                            .motionScale(0.003)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> ENDER_ESSENCE_FLUID_TYPE = registerFluidType("ender_essence_fluid",
            new BaseFluidType(WATER_STILL_RL, WATER_FLOWING_RL, WATER_OVERLAY_RL, 0xFF0F7A63,
                    setColorVector(15, 122, 99),
                    FluidType.Properties.create()
                            .viscosity(2000)
                            .density(3900)
                            .temperature(200)
                            .motionScale(0.001)
                            .lightLevel(4)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .canExtinguish(false)
                            .canHydrate(false)
                            .canConvertToSource(false)
            ));

    public static final Supplier<FluidType> MOLTEN_SOUL_SILICA_FLUID_TYPE = registerFluidType("molten_soul_silica_fluid",
            new BaseFluidType(CHUNKY_FLUID_STILL_RL, CHUNKY_FLUID_FLOWING_RL, WATER_OVERLAY_RL, 0xFF3A2E3A,
                    setColorVector(58, 46, 58),
                    FluidType.Properties.create()
                            .viscosity(5800)
                            .density(-1000)
                            .temperature(700)
                            .motionScale(0.002)
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
