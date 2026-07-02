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
                        output.accept(ModBlocks.MASTICATOR);
                        output.accept(ModBlocks.SKIN_TANK);
                        output.accept(ModBlocks.EFFLUENTCER);

                        buildBeakerContents(output);

                    }).build());

    public static final Supplier<CreativeModeTab> DERMICRAFT_ITEMS_TAB = CREATIVE_MODE_TAB.register(Dermicraft.MOD_ID + "_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SCALPEL.get()))
                    .title(Component.translatable("creativetab.dermicraft.dermicraft_items"))
                    .displayItems((itemDisplayParameters, output) -> {

                        ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModItems.SCALPEL);
                        output.accept(ModItems.SUTURE_KIT);
                        output.accept(ModItems.FORCEPS);

                        buildFlaskContents(output);
                        buildSyringeContents(output);

                        ////////////////////Parts\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModItems.EYE);
                        output.accept(ModItems.NERVE_CLUSTER);
                        output.accept(ModItems.DENSE_MUSCLE);


                        ////////////////////Buckets\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModFluids.CALCIUM_BLEND_BUCKET);
                        output.accept(ModFluids.CARBON_BLEND_BUCKET);
                        output.accept(ModFluids.PROTEIN_BLEND_BUCKET);

                        output.accept(ModFluids.CRUDE_SLURRY_BUCKET);

                        output.accept(ModFluids.PRIMITIVE_CATALYST_BUCKET);

                        output.accept(ModFluids.STONE_BLEND_BUCKET);
                        output.accept(ModFluids.SILICA_BLEND_BUCKET);
                        output.accept(ModFluids.CLAY_BLEND_BUCKET);

                        output.accept(ModFluids.FERROUS_BLEND_BUCKET);
                        output.accept(ModFluids.CUPROUS_BLEND_BUCKET);
                        output.accept(ModFluids.AUROUS_BLEND_BUCKET);
                    }).build());


    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    private static void buildSyringeContents(CreativeModeTab.Output output) {
        output.accept(ModItems.SYRINGE.get());

        //output.accept(ModItemUtil.buildSyringeStack(Fluids.WATER));
        output.accept(ModItemUtil.buildSyringeStack(Fluids.LAVA));

        //output.accept(ModItemUtil.buildSyringeStack(ModFluids.SOURCE_CALCIUM_BLEND.get()));
        output.accept(ModItemUtil.buildSyringeStack(ModFluids.SOURCE_CRUDE_SLURRY.get()));

        output.accept(ModItemUtil.buildSyringeStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get()));
    }

    private static void buildBeakerContents(CreativeModeTab.Output output) {
        output.accept(ModBlocks.BEAKER_ITEM.get());

        output.accept(ModItemUtil.buildBeakerStack(Fluids.WATER));
        output.accept(ModItemUtil.buildBeakerStack(Fluids.LAVA));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CALCIUM_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CARBON_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_PROTEIN_BLEND.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CRUDE_SLURRY.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_STONE_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_SILICA_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CLAY_BLEND.get()));

        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_FERROUS_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_CUPROUS_BLEND.get()));
        output.accept(ModItemUtil.buildBeakerStack(ModFluids.SOURCE_AUROUS_BLEND.get()));
    }

    private static void buildFlaskContents(CreativeModeTab.Output output) {
        output.accept(ModItems.GLASS_FLASK.get());

        output.accept(ModItemUtil.buildFlaskStack(Fluids.WATER));
        output.accept(ModItemUtil.buildFlaskStack(Fluids.LAVA));

        output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_CALCIUM_BLEND.get()));
        output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_CARBON_BLEND.get()));
        output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_PROTEIN_BLEND.get()));

        output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_CRUDE_SLURRY.get()));

        output.accept(ModItemUtil.buildFlaskStack(ModFluids.SOURCE_PRIMITIVE_CATALYST.get()));
    }
}
