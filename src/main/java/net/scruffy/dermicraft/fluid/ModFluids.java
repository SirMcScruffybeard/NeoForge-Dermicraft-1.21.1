package net.scruffy.dermicraft.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.function.Supplier;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, Dermicraft.MOD_ID);

    //////////////////////////////Calcium Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CALCIUM_BLEND = FLUIDS.register("source_calcium_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.CALCIUM_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CALCIUM_BLEND = FLUIDS.register("flowing_calcium_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CALCIUM_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CALCIUM_BLEND_BLOCK = ModBlocks.BLOCKS.register("calcium_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_CALCIUM_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CALCIUM_BLEND_BUCKET = getBucket("calcium_blend_bucket", ModFluids.SOURCE_CALCIUM_BLEND);

    public static final BaseFlowingFluid.Properties CALCIUM_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CALCIUM_BLEND_FLUID_TYPE, SOURCE_CALCIUM_BLEND, FLOWING_CALCIUM_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CALCIUM_BLEND_BLOCK)
            .bucket(ModFluids.CALCIUM_BLEND_BUCKET);

    //////////////////////////////Carbon Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CARBON_BLEND = FLUIDS.register("source_carbon_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.CARBON_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CARBON_BLEND = FLUIDS.register("flowing_carbon_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CARBON_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CARBON_BLEND_BLOCK = ModBlocks.BLOCKS.register("carbon_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_CARBON_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CARBON_BLEND_BUCKET = getBucket("carbon_blend_bucket", ModFluids.SOURCE_CARBON_BLEND);

    public static final BaseFlowingFluid.Properties CARBON_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CARBON_BLEND_FLUID_TYPE, SOURCE_CARBON_BLEND, FLOWING_CARBON_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CARBON_BLEND_BLOCK)
            .bucket(ModFluids.CARBON_BLEND_BUCKET);

    //////////////////////////////Protein Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_PROTEIN_BLEND = FLUIDS.register("source_protein_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.PROTEIN_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_PROTEIN_BLEND = FLUIDS.register("flowing_protein_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.PROTEIN_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> PROTEIN_BLEND_BLOCK = ModBlocks.BLOCKS.register("protein_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_PROTEIN_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> PROTEIN_BLEND_BUCKET = getBucket("protein_blend_bucket", ModFluids.SOURCE_PROTEIN_BLEND);

    public static final BaseFlowingFluid.Properties PROTEIN_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.PROTEIN_BLEND_FLUID_TYPE, SOURCE_PROTEIN_BLEND, FLOWING_PROTEIN_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.PROTEIN_BLEND_BLOCK)
            .bucket(ModFluids.PROTEIN_BLEND_BUCKET);

    //////////////////////////////Primitive Catalyst\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_PRIMITIVE_CATALYST = FLUIDS.register("source_primitive_catalyst",
            () -> new BaseFlowingFluid.Source(ModFluids.PRIMITIVE_CATALYST_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_PRIMITIVE_CATALYST = FLUIDS.register("flowing_primitive_catalyst",
            () -> new BaseFlowingFluid.Flowing(ModFluids.PRIMITIVE_CATALYST_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> PRIMITIVE_CATALYST_BLOCK = ModBlocks.BLOCKS.register("primitive_catalyst_block",
            () -> new LiquidBlock(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> PRIMITIVE_CATALYST_BUCKET = ModItems.ITEMS.registerItem("primitive_catalyst_bucket",
            properties -> new BucketItem(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), properties
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));

    public static final BaseFlowingFluid.Properties PRIMITIVE_CATALYST_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.PRIMITIVE_CATALYST_FLUID_TYPE, SOURCE_PRIMITIVE_CATALYST, FLOWING_PRIMITIVE_CATALYST)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.PRIMITIVE_CATALYST_BLOCK)
            .bucket(ModFluids.PRIMITIVE_CATALYST_BUCKET);

    //////////////////////////////Crude Slurry\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CRUDE_SLURRY = FLUIDS.register("source_crude_slurry",
            () -> new BaseFlowingFluid.Source(ModFluids.CRUDE_SLURRY_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CRUDE_SLURRY = FLUIDS.register("flowing_crude_slurry",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CRUDE_SLURRY_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CRUDE_SLURRY_BLOCK = ModBlocks.BLOCKS.register("crude_slurry_block",
            () -> new LiquidBlock(ModFluids.SOURCE_CRUDE_SLURRY.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CRUDE_SLURRY_BUCKET = getBucket("crude_slurry_bucket", ModFluids.SOURCE_CRUDE_SLURRY);

    public static final BaseFlowingFluid.Properties CRUDE_SLURRY_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CRUDE_SLURRY_FLUID_TYPE, SOURCE_CRUDE_SLURRY, FLOWING_CRUDE_SLURRY)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CRUDE_SLURRY_BLOCK)
            .bucket(ModFluids.CRUDE_SLURRY_BUCKET);

    //////////////////////////////Concentrated Slurry\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CONCENTRATED_SLURRY = FLUIDS.register("source_concentrated_slurry",
            () -> new BaseFlowingFluid.Source(ModFluids.CONCENTRATED_SLURRY_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CONCENTRATED_SLURRY = FLUIDS.register("flowing_concentrated_slurry",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CONCENTRATED_SLURRY_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CONCENTRATED_SLURRY_BLOCK = ModBlocks.BLOCKS.register("concentrated_slurry_block",
            () -> new LiquidBlock(ModFluids.SOURCE_CONCENTRATED_SLURRY.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CONCENTRATED_SLURRY_BUCKET = getBucket("concentrated_slurry_bucket", ModFluids.SOURCE_CONCENTRATED_SLURRY);

    public static final BaseFlowingFluid.Properties CONCENTRATED_SLURRY_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CONCENTRATED_SLURRY_FLUID_TYPE, SOURCE_CONCENTRATED_SLURRY, FLOWING_CONCENTRATED_SLURRY)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CONCENTRATED_SLURRY_BLOCK)
            .bucket(ModFluids.CONCENTRATED_SLURRY_BUCKET);

    //////////////////////////////Stone Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_STONE_BLEND = FLUIDS.register("source_stone_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.STONE_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_STONE_BLEND = FLUIDS.register("flowing_stone_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.STONE_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> STONE_BLEND_BLOCK = ModBlocks.BLOCKS.register("stone_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_STONE_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> STONE_BLEND_BUCKET = getBucket("stone_blend_bucket", ModFluids.SOURCE_STONE_BLEND);

    public static final BaseFlowingFluid.Properties STONE_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.STONE_BLEND_FLUID_TYPE, SOURCE_STONE_BLEND, FLOWING_STONE_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.STONE_BLEND_BLOCK)
            .bucket(ModFluids.STONE_BLEND_BUCKET);

    //////////////////////////////Silica Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_SILICA_BLEND = FLUIDS.register("source_silica_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.SILICA_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_SILICA_BLEND = FLUIDS.register("flowing_silica_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.SILICA_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> SILICA_BLEND_BLOCK = ModBlocks.BLOCKS.register("silica_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_SILICA_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> SILICA_BLEND_BUCKET = getBucket("silica_blend_bucket", ModFluids.SOURCE_SILICA_BLEND);

    public static final BaseFlowingFluid.Properties SILICA_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.SILICA_BLEND_FLUID_TYPE, SOURCE_SILICA_BLEND, FLOWING_SILICA_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.SILICA_BLEND_BLOCK)
            .bucket(ModFluids.SILICA_BLEND_BUCKET);

    //////////////////////////////Clay Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CLAY_BLEND = FLUIDS.register("source_clay_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.CLAY_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CLAY_BLEND = FLUIDS.register("flowing_clay_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CLAY_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CLAY_BLEND_BLOCK = ModBlocks.BLOCKS.register("clay_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_CLAY_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CLAY_BLEND_BUCKET = getBucket("clay_blend_bucket", ModFluids.SOURCE_CLAY_BLEND);

    public static final BaseFlowingFluid.Properties CLAY_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CLAY_BLEND_FLUID_TYPE, SOURCE_CLAY_BLEND, FLOWING_CLAY_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CLAY_BLEND_BLOCK)
            .bucket(ModFluids.CLAY_BLEND_BUCKET);

    //////////////////////////////Ferrous Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Base metal: Iron
    public static final Supplier<FlowingFluid> SOURCE_FERROUS_BLEND = FLUIDS.register("source_ferrous_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.FERROUS_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_FERROUS_BLEND = FLUIDS.register("flowing_ferrous_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.FERROUS_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> FERROUS_BLEND_BLOCK = ModBlocks.BLOCKS.register("ferrous_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_FERROUS_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> FERROUS_BLEND_BUCKET = getBucket("ferrous_blend_bucket", ModFluids.SOURCE_FERROUS_BLEND);

    public static final BaseFlowingFluid.Properties FERROUS_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.FERROUS_BLEND_FLUID_TYPE, SOURCE_FERROUS_BLEND, FLOWING_FERROUS_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.FERROUS_BLEND_BLOCK)
            .bucket(ModFluids.FERROUS_BLEND_BUCKET);

    //////////////////////////////Cuprous Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Base metal: Copper
    public static final Supplier<FlowingFluid> SOURCE_CUPROUS_BLEND = FLUIDS.register("source_cuprous_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.CUPROUS_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CUPROUS_BLEND = FLUIDS.register("flowing_cuprous_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CUPROUS_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CUPROUS_BLEND_BLOCK = ModBlocks.BLOCKS.register("cuprous_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_CUPROUS_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CUPROUS_BLEND_BUCKET = getBucket("cuprous_blend_bucket", ModFluids.SOURCE_CUPROUS_BLEND);

    public static final BaseFlowingFluid.Properties CUPROUS_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CUPROUS_BLEND_FLUID_TYPE, SOURCE_CUPROUS_BLEND, FLOWING_CUPROUS_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CUPROUS_BLEND_BLOCK)
            .bucket(ModFluids.CUPROUS_BLEND_BUCKET);

    //////////////////////////////Aurous Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Base metal: Gold
    public static final Supplier<FlowingFluid> SOURCE_AUROUS_BLEND = FLUIDS.register("source_aurous_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.AUROUS_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_AUROUS_BLEND = FLUIDS.register("flowing_aurous_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.AUROUS_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> AUROUS_BLEND_BLOCK = ModBlocks.BLOCKS.register("aurous_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_AUROUS_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> AUROUS_BLEND_BUCKET = getBucket("aurous_blend_bucket", ModFluids.SOURCE_AUROUS_BLEND);

    public static final BaseFlowingFluid.Properties AUROUS_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.AUROUS_BLEND_FLUID_TYPE, SOURCE_AUROUS_BLEND, FLOWING_AUROUS_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.AUROUS_BLEND_BLOCK)
            .bucket(ModFluids.AUROUS_BLEND_BUCKET);

    //////////////////////////////F-Stuff\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_F_STUFF = FLUIDS.register("source_f_stuff",
            () -> new BaseFlowingFluid.Source(ModFluids.F_STUFF_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_F_STUFF = FLUIDS.register("flowing_f_stuff",
            () -> new BaseFlowingFluid.Flowing(ModFluids.F_STUFF_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> F_STUFF_BLOCK = ModBlocks.BLOCKS.register("f_stuff_block",
            () -> new LiquidBlock(ModFluids.SOURCE_F_STUFF.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> F_STUFF_BUCKET = getBucket("f_stuff_bucket", ModFluids.SOURCE_F_STUFF);

    public static final BaseFlowingFluid.Properties F_STUFF_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.F_STUFF_FLUID_TYPE, SOURCE_F_STUFF, FLOWING_F_STUFF)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.F_STUFF_BLOCK)
            .bucket(ModFluids.F_STUFF_BUCKET);

    //////////////////////////////C-Stuff\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_C_STUFF = FLUIDS.register("source_c_stuff",
            () -> new BaseFlowingFluid.Source(ModFluids.C_STUFF_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_C_STUFF = FLUIDS.register("flowing_c_stuff",
            () -> new BaseFlowingFluid.Flowing(ModFluids.C_STUFF_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> C_STUFF_BLOCK = ModBlocks.BLOCKS.register("c_stuff_block",
            () -> new LiquidBlock(ModFluids.SOURCE_C_STUFF.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> C_STUFF_BUCKET = getBucket("c_stuff_bucket", ModFluids.SOURCE_C_STUFF);

    public static final BaseFlowingFluid.Properties C_STUFF_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.C_STUFF_FLUID_TYPE, SOURCE_C_STUFF, FLOWING_C_STUFF)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.C_STUFF_BLOCK)
            .bucket(ModFluids.C_STUFF_BUCKET);

    //////////////////////////////Pulp Blend\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_PULP_BLEND = FLUIDS.register("source_pulp_blend",
            () -> new BaseFlowingFluid.Source(ModFluids.PULP_BLEND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_PULP_BLEND = FLUIDS.register("flowing_pulp_blend",
            () -> new BaseFlowingFluid.Flowing(ModFluids.PULP_BLEND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> PULP_BLEND_BLOCK = ModBlocks.BLOCKS.register("pulp_blend_block",
            () -> new LiquidBlock(ModFluids.SOURCE_PULP_BLEND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> PULP_BLEND_BUCKET = getBucket("pulp_blend_bucket", ModFluids.SOURCE_PULP_BLEND);

    public static final BaseFlowingFluid.Properties PULP_BLEND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.PULP_BLEND_FLUID_TYPE, SOURCE_PULP_BLEND, FLOWING_PULP_BLEND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.PULP_BLEND_BLOCK)
            .bucket(ModFluids.PULP_BLEND_BUCKET);

    //////////////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }

    private static DeferredItem<Item> getBucket(String name, Supplier<FlowingFluid> fluid) {
        return ModItems.ITEMS.registerItem(name, properties -> new BucketItem(fluid.get(), properties
                .craftRemainder(Items.BUCKET).stacksTo(1)));
    }
}
