package net.scruffy.dermicraft.main;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.util.ModItemUtil;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dermicraft.MOD_ID);

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }

    public static final Supplier<CreativeModeTab> DERMICRAFT_BLOCKS_TAB = CREATIVE_MODE_TAB.register(Dermicraft.MOD_ID + "_blocks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.INERT_TUMOR.get()))
                    .title(Component.translatable("creativetab.dermicraft.dermicraft_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {

                        ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModBlocks.INERT_TUMOR);
                        output.accept(ModBlocks.MARRED_TUMOR);
                        output.accept(ModBlocks.STITCHED_TUMOR);

                        output.accept(ModBlocks.EYE_TUMOR);
                        output.accept(ModBlocks.MUSCLE_TUMOR);
                        output.accept(ModBlocks.NERVE_TUMOR);

                        ////////////////////Machines and Tanks\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModBlocks.CALCIUM_GLASS);
                        output.accept(ModBlocks.OUTERFACE);
                        output.accept(ModBlocks.DROOLING_CAULDRON);
                        // No craft recipe yet -- standalone-reachable Evolution/Catalyst path isn't
                        // built (dermicraft-machine-notes.md). Visible in creative for testing only.
                        output.accept(ModBlocks.DROOLING_CRUCIBLE);
                        output.accept(ModBlocks.MASTICATOR);
                        output.accept(ModBlocks.SKIN_TANK);
                        output.accept(ModBlocks.EFFLUENTCER);
                        output.accept(ModBlocks.METASTASIZER);
                        output.accept(ModBlocks.MUTATOR);
                        output.accept(ModBlocks.RENDER_FURNACE);
                        output.accept(ModBlocks.GRAFTING_TABLE);
                        output.accept(ModBlocks.RENDER_KILN);
                        output.accept(ModBlocks.CRAW);
                        output.accept(ModBlocks.MR_FARMER);
                        output.accept(ModBlocks.MR_SHEPARD);
                        output.accept(ModBlocks.BRAIN);
                        // WORKBENCH_TOP is deliberately not offered here -- it's not meant to be
                        // placed by hand, WorkbenchBlock#setPlacedBy auto-places it above the bottom.
                        output.accept(ModBlocks.WORKBENCH);

                        ////////////////////Innards Duct\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModBlocks.INNARDS_DUCT);
                        output.accept(ModBlocks.INNARDS_NODE);

                        ////////////////////Gate\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModBlocks.INNARDS_GATE_CONTROLLER);
                        output.accept(ModBlocks.INNARDS_GATE_BUFFER);
                        output.accept(ModBlocks.INNARDS_GATE_PORT);

                        ////////////////////Flesh Lab Floor\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModBlocks.STONE_LAB_FLOOR);
                        output.accept(ModBlocks.COBBLESTONE_LAB_FLOOR);
                        output.accept(ModBlocks.DEEPSLATE_LAB_FLOOR);
                        output.accept(ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR);
                        output.accept(ModBlocks.DIORITE_LAB_FLOOR);
                        output.accept(ModBlocks.ANDESITE_LAB_FLOOR);
                        output.accept(ModBlocks.GRANITE_LAB_FLOOR);

                        buildBeakerContents(output);

                    }).build());

    public static final Supplier<CreativeModeTab> DERMICRAFT_ITEMS_TAB = CREATIVE_MODE_TAB.register(Dermicraft.MOD_ID + "_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SCALPEL.get()))
                    .title(Component.translatable("creativetab.dermicraft.dermicraft_items"))
                    .displayItems((itemDisplayParameters, output) -> {

                        ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModItems.SCALPEL);
                        output.accept(ModItems.PRIMITIVE_SCALPEL);
                        output.accept(ModItems.SUTURE_KIT);
                        output.accept(ModItems.PRIMITIVE_SUTURE_KIT);
                        output.accept(ModItems.FORCEPS);
                        output.accept(ModItems.PRIMITIVE_FORCEPS);
                        output.accept(ModItems.IDEP);
                        output.accept(ModItems.SCRENCH);
                        output.accept(ModItems.DRINKER);
                        output.accept(ModItems.EATER);
                        output.accept(ModItems.SIPPING);
                        output.accept(ModItems.SUNDER);
                        output.accept(ModItems.SHATTER);
                        output.accept(ModItems.BONE_SHATTER_HEAD);
                        output.accept(ModItems.COPPER_SHATTER_HEAD);
                        output.accept(ModItems.GOLD_SHATTER_HEAD);
                        output.accept(ModItems.IRON_SHATTER_HEAD);
                        output.accept(ModItems.DIAMOND_SHATTER_HEAD);
                        output.accept(ModItems.BLADDER);
                        output.accept(ModItems.FUEL_BLADDER);
                        output.accept(ModItems.FEEDER_BLADDER);
                        output.accept(ModItems.IRON_SUNDER_CHAIN);
                        output.accept(ModItems.COPPER_SUNDER_CHAIN);
                        output.accept(ModItems.GOLD_SUNDER_CHAIN);
                        output.accept(ModItems.DIAMOND_SUNDER_CHAIN);
                        output.accept(ModItems.BONE_SUNDER_CHAIN);

                        ////////////////////Gadget Modules\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModItems.AGGREGATE_MODULE);
                        output.accept(ModItems.BEAM_MODULE);
                        output.accept(ModItems.FLUID_BYPASS_MODULE);
                        output.accept(ModItems.HEAT_SAFETY_MODULE);
                        output.accept(ModItems.HEAT_EVOLUTION_MODULE); // no mechanics yet, item only
                        output.accept(ModItems.METAPHYSICAL_SAFETY_MODULE);

                        buildFlaskContents(output);
                        buildSyringeContents(output);

                        ////////////////////Parts\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModItems.EYE);
                        output.accept(ModItems.NERVE_CLUSTER);
                        output.accept(ModItems.DENSE_MUSCLE);
                        output.accept(ModItems.BLOOD_NUGGET);
                        output.accept(ModItems.PROTO_BRAIN);
                        output.accept(ModItems.CHASSIS);
                        output.accept(ModItems.MODULE_FRAME);

                        ////////////////////Food\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModItems.MRE);
                        output.accept(ModItems.MEAT_FLAVORED_MEAT);


                        ////////////////////Buckets\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModFluids.CALCIUM_BLEND_BUCKET);
                        output.accept(ModFluids.CARBON_BLEND_BUCKET);
                        output.accept(ModFluids.PROTEIN_BLEND_BUCKET);
                        output.accept(ModFluids.F_STUFF_BUCKET);
                        output.accept(ModFluids.C_STUFF_BUCKET);

                        output.accept(ModFluids.CRUDE_SLURRY_BUCKET);
                        output.accept(ModFluids.CONCENTRATED_SLURRY_BUCKET);

                        output.accept(ModFluids.PRIMITIVE_CATALYST_BUCKET);
                        output.accept(ModFluids.REINFORCING_CATALYST_BUCKET);
                        output.accept(ModFluids.SYNAPSE_CATALYST_BUCKET);

                        output.accept(ModFluids.STONE_BLEND_BUCKET);
                        output.accept(ModFluids.SILICA_BLEND_BUCKET);
                        output.accept(ModFluids.CLAY_BLEND_BUCKET);

                        output.accept(ModFluids.FERROUS_BLEND_BUCKET);
                        output.accept(ModFluids.CUPROUS_BLEND_BUCKET);
                        output.accept(ModFluids.AUROUS_BLEND_BUCKET);

                        output.accept(ModFluids.PULP_BLEND_BUCKET);

                        output.accept(ModFluids.MOLTEN_REDSTONE_BUCKET);
                        output.accept(ModFluids.MOLTEN_QUARTZ_BUCKET);
                        output.accept(ModFluids.MOLTEN_GLOWSTONE_BUCKET);
                        output.accept(ModFluids.MOLTEN_AMETHYST_BUCKET);
                        output.accept(ModFluids.MOLTEN_DIAMOND_BUCKET);                        output.accept(ModFluids.MOLTEN_LAPIS_BUCKET);
                        output.accept(ModFluids.MOLTEN_RAW_NETHERITE_SCRAP_BUCKET);
                        output.accept(ModFluids.MOLTEN_NETHERITE_BUCKET);
                        output.accept(ModFluids.BLAZE_ESSENCE_BUCKET);
                        output.accept(ModFluids.GHAST_ESSENCE_BUCKET);
                        output.accept(ModFluids.WITHER_ESSENCE_BUCKET);
                        output.accept(ModFluids.ENDER_ESSENCE_BUCKET);
                        output.accept(ModFluids.MOLTEN_SOUL_SILICA_BUCKET);


                    }).build());


    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    private static void buildSyringeContents(CreativeModeTab.Output output) {
        output.accept(ModItems.SYRINGE.get());
        output.accept(ModItems.PRIMITIVE_SYRINGE.get());

        //output.accept(ModItemUtil.buildSyringeStack(Fluids.WATER));
        //output.accept(ModItemUtil.buildSyringeStack(Fluids.LAVA));

        //output.accept(ModItemUtil.buildSyringeStack(ModFluids.SOURCE_CALCIUM_BLEND.get()));
        output.accept(ModItemUtil.buildSyringeStack(ModFluids.SOURCE_CRUDE_SLURRY.get()));

        output.accept(ModItemUtil.buildSyringeStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get()));
        output.accept(ModItemUtil.buildSyringeStack(ModFluids.SOURCE_SYNAPSE_CATALYST.get()));
    }

    private static void buildBeakerContents(CreativeModeTab.Output output) {
        output.accept(ModBlocks.BEAKER_ITEM.get());

        output.accept(ModItemUtil.buildBeakerStack(Fluids.WATER));
        output.accept(ModItemUtil.buildBeakerStack(Fluids.LAVA));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CALCIUM_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CARBON_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_PROTEIN_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_F_STUFF.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_C_STUFF.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CRUDE_SLURRY.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CONCENTRATED_SLURRY.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_REINFORCING_CATALYST.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_SYNAPSE_CATALYST.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_STONE_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_SILICA_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CLAY_BLEND.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_PULP_BLEND.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_FERROUS_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CUPROUS_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_AUROUS_BLEND.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_REDSTONE.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_QUARTZ.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_GLOWSTONE.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_AMETHYST.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_DIAMOND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_LAPIS.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_RAW_NETHERITE_SCRAP.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_NETHERITE.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_BLAZE_ESSENCE.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_GHAST_ESSENCE.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_WITHER_ESSENCE.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_ENDER_ESSENCE.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_MOLTEN_SOUL_SILICA.get()));

    }

    private static void buildFlaskContents(CreativeModeTab.Output output) {
        output.accept(ModItems.GLASS_FLASK.get());

        output.accept(ModItemUtil.buildFlaskStack(Fluids.WATER));
        output.accept(ModItemUtil.buildFlaskStack(Fluids.LAVA));

        //output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_CALCIUM_BLEND.get()));
        output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_CARBON_BLEND.get()));
        //output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_PROTEIN_BLEND.get()));

        output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_CRUDE_SLURRY.get()));

        //output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get()));
    }
}
