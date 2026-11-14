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
import net.scruffy.dermicraft.block.custom.ModLiquidBlock;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.function.Supplier;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, Dermicraft.MOD_ID);

    //////////////////////////////Calcium Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CALCIUM_PULP = FLUIDS.register("source_calcium_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.CALCIUM_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CALCIUM_PULP = FLUIDS.register("flowing_calcium_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CALCIUM_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CALCIUM_PULP_BLOCK = ModBlocks.BLOCKS.register("calcium_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_CALCIUM_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CALCIUM_PULP_BUCKET = getBucket("calcium_pulp_bucket", ModFluids.SOURCE_CALCIUM_PULP);

    public static final BaseFlowingFluid.Properties CALCIUM_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CALCIUM_PULP_FLUID_TYPE, SOURCE_CALCIUM_PULP, FLOWING_CALCIUM_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CALCIUM_PULP_BLOCK)
            .bucket(ModFluids.CALCIUM_PULP_BUCKET);

    //////////////////////////////Carbon Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CARBON_PULP = FLUIDS.register("source_carbon_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.CARBON_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CARBON_PULP = FLUIDS.register("flowing_carbon_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CARBON_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CARBON_PULP_BLOCK = ModBlocks.BLOCKS.register("carbon_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_CARBON_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CARBON_PULP_BUCKET = getBucket("carbon_pulp_bucket", ModFluids.SOURCE_CARBON_PULP);

    public static final BaseFlowingFluid.Properties CARBON_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CARBON_PULP_FLUID_TYPE, SOURCE_CARBON_PULP, FLOWING_CARBON_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CARBON_PULP_BLOCK)
            .bucket(ModFluids.CARBON_PULP_BUCKET);

    //////////////////////////////Protein Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_PROTEIN_PULP = FLUIDS.register("source_protein_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.PROTEIN_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_PROTEIN_PULP = FLUIDS.register("flowing_protein_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.PROTEIN_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> PROTEIN_PULP_BLOCK = ModBlocks.BLOCKS.register("protein_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_PROTEIN_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> PROTEIN_PULP_BUCKET = getBucket("protein_pulp_bucket", ModFluids.SOURCE_PROTEIN_PULP);

    public static final BaseFlowingFluid.Properties PROTEIN_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.PROTEIN_PULP_FLUID_TYPE, SOURCE_PROTEIN_PULP, FLOWING_PROTEIN_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.PROTEIN_PULP_BLOCK)
            .bucket(ModFluids.PROTEIN_PULP_BUCKET);

    //////////////////////////////Primitive Catalyst\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_PRIMITIVE_CATALYST = FLUIDS.register("source_primitive_catalyst",
            () -> new BaseFlowingFluid.Source(ModFluids.PRIMITIVE_CATALYST_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_PRIMITIVE_CATALYST = FLUIDS.register("flowing_primitive_catalyst",
            () -> new BaseFlowingFluid.Flowing(ModFluids.PRIMITIVE_CATALYST_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> PRIMITIVE_CATALYST_BLOCK = ModBlocks.BLOCKS.register("primitive_catalyst_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> PRIMITIVE_CATALYST_BUCKET = ModItems.ITEMS.registerItem("primitive_catalyst_bucket",
            properties -> new BucketItem(ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), properties
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));

    public static final BaseFlowingFluid.Properties PRIMITIVE_CATALYST_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.PRIMITIVE_CATALYST_FLUID_TYPE, SOURCE_PRIMITIVE_CATALYST, FLOWING_PRIMITIVE_CATALYST)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.PRIMITIVE_CATALYST_BLOCK)
            .bucket(ModFluids.PRIMITIVE_CATALYST_BUCKET);

    //////////////////////////////Reinforcing Catalyst\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_REINFORCING_CATALYST = FLUIDS.register("source_reinforcing_catalyst",
            () -> new BaseFlowingFluid.Source(ModFluids.REINFORCING_CATALYST_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_REINFORCING_CATALYST = FLUIDS.register("flowing_reinforcing_catalyst",
            () -> new BaseFlowingFluid.Flowing(ModFluids.REINFORCING_CATALYST_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> REINFORCING_CATALYST_BLOCK = ModBlocks.BLOCKS.register("reinforcing_catalyst_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_REINFORCING_CATALYST.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> REINFORCING_CATALYST_BUCKET = getBucket("reinforcing_catalyst_bucket", ModFluids.SOURCE_REINFORCING_CATALYST);

    public static final BaseFlowingFluid.Properties REINFORCING_CATALYST_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.REINFORCING_CATALYST_FLUID_TYPE, SOURCE_REINFORCING_CATALYST, FLOWING_REINFORCING_CATALYST)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.REINFORCING_CATALYST_BLOCK)
            .bucket(ModFluids.REINFORCING_CATALYST_BUCKET);

    //////////////////////////////Synapse Catalyst\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_SYNAPSE_CATALYST = FLUIDS.register("source_synapse_catalyst",
            () -> new BaseFlowingFluid.Source(ModFluids.SYNAPSE_CATALYST_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_SYNAPSE_CATALYST = FLUIDS.register("flowing_synapse_catalyst",
            () -> new BaseFlowingFluid.Flowing(ModFluids.SYNAPSE_CATALYST_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> SYNAPSE_CATALYST_BLOCK = ModBlocks.BLOCKS.register("synapse_catalyst_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_SYNAPSE_CATALYST.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> SYNAPSE_CATALYST_BUCKET = getBucket("synapse_catalyst_bucket", ModFluids.SOURCE_SYNAPSE_CATALYST);

    public static final BaseFlowingFluid.Properties SYNAPSE_CATALYST_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.SYNAPSE_CATALYST_FLUID_TYPE, SOURCE_SYNAPSE_CATALYST, FLOWING_SYNAPSE_CATALYST)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.SYNAPSE_CATALYST_BLOCK)
            .bucket(ModFluids.SYNAPSE_CATALYST_BUCKET);

    //////////////////////////////Knowledge Colloid\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_KNOWLEDGE_COLLOID = FLUIDS.register("source_knowledge_colloid",
            () -> new BaseFlowingFluid.Source(ModFluids.KNOWLEDGE_COLLOID_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_KNOWLEDGE_COLLOID = FLUIDS.register("flowing_knowledge_colloid",
            () -> new BaseFlowingFluid.Flowing(ModFluids.KNOWLEDGE_COLLOID_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> KNOWLEDGE_COLLOID_BLOCK = ModBlocks.BLOCKS.register("knowledge_colloid_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_KNOWLEDGE_COLLOID.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .lightLevel(state -> 8).noLootTable()));

    public static final DeferredItem<Item> KNOWLEDGE_COLLOID_BUCKET = getBucket("knowledge_colloid_bucket", ModFluids.SOURCE_KNOWLEDGE_COLLOID);

    public static final BaseFlowingFluid.Properties KNOWLEDGE_COLLOID_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.KNOWLEDGE_COLLOID_FLUID_TYPE, SOURCE_KNOWLEDGE_COLLOID, FLOWING_KNOWLEDGE_COLLOID)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.KNOWLEDGE_COLLOID_BLOCK)
            .bucket(ModFluids.KNOWLEDGE_COLLOID_BUCKET);

    //////////////////////////////Kinetic Catalyst\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_KINETIC_CATALYST = FLUIDS.register("source_kinetic_catalyst",
            () -> new BaseFlowingFluid.Source(ModFluids.KINETIC_CATALYST_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_KINETIC_CATALYST = FLUIDS.register("flowing_kinetic_catalyst",
            () -> new BaseFlowingFluid.Flowing(ModFluids.KINETIC_CATALYST_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> KINETIC_CATALYST_BLOCK = ModBlocks.BLOCKS.register("kinetic_catalyst_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_KINETIC_CATALYST.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> KINETIC_CATALYST_BUCKET = getBucket("kinetic_catalyst_bucket", ModFluids.SOURCE_KINETIC_CATALYST);

    public static final BaseFlowingFluid.Properties KINETIC_CATALYST_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.KINETIC_CATALYST_FLUID_TYPE, SOURCE_KINETIC_CATALYST, FLOWING_KINETIC_CATALYST)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.KINETIC_CATALYST_BLOCK)
            .bucket(ModFluids.KINETIC_CATALYST_BUCKET);

    //////////////////////////////Evolution Catalyst\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_EVOLUTION_CATALYST = FLUIDS.register("source_evolution_catalyst",
            () -> new BaseFlowingFluid.Source(ModFluids.EVOLUTION_CATALYST_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_EVOLUTION_CATALYST = FLUIDS.register("flowing_evolution_catalyst",
            () -> new BaseFlowingFluid.Flowing(ModFluids.EVOLUTION_CATALYST_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> EVOLUTION_CATALYST_BLOCK = ModBlocks.BLOCKS.register("evolution_catalyst_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_EVOLUTION_CATALYST.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .lightLevel(state -> 7).noLootTable()));

    public static final DeferredItem<Item> EVOLUTION_CATALYST_BUCKET = getBucket("evolution_catalyst_bucket", ModFluids.SOURCE_EVOLUTION_CATALYST);

    public static final BaseFlowingFluid.Properties EVOLUTION_CATALYST_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.EVOLUTION_CATALYST_FLUID_TYPE, SOURCE_EVOLUTION_CATALYST, FLOWING_EVOLUTION_CATALYST)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.EVOLUTION_CATALYST_BLOCK)
            .bucket(ModFluids.EVOLUTION_CATALYST_BUCKET);

    //////////////////////////////Crude Slurry\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CRUDE_SLURRY = FLUIDS.register("source_crude_slurry",
            () -> new BaseFlowingFluid.Source(ModFluids.CRUDE_SLURRY_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CRUDE_SLURRY = FLUIDS.register("flowing_crude_slurry",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CRUDE_SLURRY_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CRUDE_SLURRY_BLOCK = ModBlocks.BLOCKS.register("crude_slurry_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_CRUDE_SLURRY.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

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
            () -> new ModLiquidBlock(ModFluids.SOURCE_CONCENTRATED_SLURRY.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CONCENTRATED_SLURRY_BUCKET = getBucket("concentrated_slurry_bucket", ModFluids.SOURCE_CONCENTRATED_SLURRY);

    public static final BaseFlowingFluid.Properties CONCENTRATED_SLURRY_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CONCENTRATED_SLURRY_FLUID_TYPE, SOURCE_CONCENTRATED_SLURRY, FLOWING_CONCENTRATED_SLURRY)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CONCENTRATED_SLURRY_BLOCK)
            .bucket(ModFluids.CONCENTRATED_SLURRY_BUCKET);

    //////////////////////////////Stone Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_STONE_PULP = FLUIDS.register("source_stone_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.STONE_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_STONE_PULP = FLUIDS.register("flowing_stone_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.STONE_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> STONE_PULP_BLOCK = ModBlocks.BLOCKS.register("stone_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_STONE_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> STONE_PULP_BUCKET = getBucket("stone_pulp_bucket", ModFluids.SOURCE_STONE_PULP);

    public static final BaseFlowingFluid.Properties STONE_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.STONE_PULP_FLUID_TYPE, SOURCE_STONE_PULP, FLOWING_STONE_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.STONE_PULP_BLOCK)
            .bucket(ModFluids.STONE_PULP_BUCKET);

    //////////////////////////////Silica Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_SILICA_PULP = FLUIDS.register("source_silica_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.SILICA_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_SILICA_PULP = FLUIDS.register("flowing_silica_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.SILICA_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> SILICA_PULP_BLOCK = ModBlocks.BLOCKS.register("silica_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_SILICA_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> SILICA_PULP_BUCKET = getBucket("silica_pulp_bucket", ModFluids.SOURCE_SILICA_PULP);

    public static final BaseFlowingFluid.Properties SILICA_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.SILICA_PULP_FLUID_TYPE, SOURCE_SILICA_PULP, FLOWING_SILICA_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.SILICA_PULP_BLOCK)
            .bucket(ModFluids.SILICA_PULP_BUCKET);

    //////////////////////////////Clay Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CLAY_PULP = FLUIDS.register("source_clay_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.CLAY_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CLAY_PULP = FLUIDS.register("flowing_clay_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CLAY_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CLAY_PULP_BLOCK = ModBlocks.BLOCKS.register("clay_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_CLAY_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CLAY_PULP_BUCKET = getBucket("clay_pulp_bucket", ModFluids.SOURCE_CLAY_PULP);

    public static final BaseFlowingFluid.Properties CLAY_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CLAY_PULP_FLUID_TYPE, SOURCE_CLAY_PULP, FLOWING_CLAY_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CLAY_PULP_BLOCK)
            .bucket(ModFluids.CLAY_PULP_BUCKET);

    //////////////////////////////Ferrous Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Base metal: Iron
    public static final Supplier<FlowingFluid> SOURCE_FERROUS_PULP = FLUIDS.register("source_ferrous_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.FERROUS_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_FERROUS_PULP = FLUIDS.register("flowing_ferrous_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.FERROUS_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> FERROUS_PULP_BLOCK = ModBlocks.BLOCKS.register("ferrous_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_FERROUS_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> FERROUS_PULP_BUCKET = getBucket("ferrous_pulp_bucket", ModFluids.SOURCE_FERROUS_PULP);

    public static final BaseFlowingFluid.Properties FERROUS_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.FERROUS_PULP_FLUID_TYPE, SOURCE_FERROUS_PULP, FLOWING_FERROUS_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.FERROUS_PULP_BLOCK)
            .bucket(ModFluids.FERROUS_PULP_BUCKET);

    //////////////////////////////Cuprous Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Base metal: Copper
    public static final Supplier<FlowingFluid> SOURCE_CUPROUS_PULP = FLUIDS.register("source_cuprous_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.CUPROUS_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CUPROUS_PULP = FLUIDS.register("flowing_cuprous_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CUPROUS_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CUPROUS_PULP_BLOCK = ModBlocks.BLOCKS.register("cuprous_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_CUPROUS_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CUPROUS_PULP_BUCKET = getBucket("cuprous_pulp_bucket", ModFluids.SOURCE_CUPROUS_PULP);

    public static final BaseFlowingFluid.Properties CUPROUS_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CUPROUS_PULP_FLUID_TYPE, SOURCE_CUPROUS_PULP, FLOWING_CUPROUS_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CUPROUS_PULP_BLOCK)
            .bucket(ModFluids.CUPROUS_PULP_BUCKET);

    //////////////////////////////Aurous Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    // Base metal: Gold
    public static final Supplier<FlowingFluid> SOURCE_AUROUS_PULP = FLUIDS.register("source_aurous_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.AUROUS_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_AUROUS_PULP = FLUIDS.register("flowing_aurous_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.AUROUS_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> AUROUS_PULP_BLOCK = ModBlocks.BLOCKS.register("aurous_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_AUROUS_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> AUROUS_PULP_BUCKET = getBucket("aurous_pulp_bucket", ModFluids.SOURCE_AUROUS_PULP);

    public static final BaseFlowingFluid.Properties AUROUS_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.AUROUS_PULP_FLUID_TYPE, SOURCE_AUROUS_PULP, FLOWING_AUROUS_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.AUROUS_PULP_BLOCK)
            .bucket(ModFluids.AUROUS_PULP_BUCKET);

    //////////////////////////////F-Stuff\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_F_STUFF = FLUIDS.register("source_f_stuff",
            () -> new BaseFlowingFluid.Source(ModFluids.F_STUFF_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_F_STUFF = FLUIDS.register("flowing_f_stuff",
            () -> new BaseFlowingFluid.Flowing(ModFluids.F_STUFF_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> F_STUFF_BLOCK = ModBlocks.BLOCKS.register("f_stuff_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_F_STUFF.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

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
            () -> new ModLiquidBlock(ModFluids.SOURCE_C_STUFF.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> C_STUFF_BUCKET = getBucket("c_stuff_bucket", ModFluids.SOURCE_C_STUFF);

    public static final BaseFlowingFluid.Properties C_STUFF_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.C_STUFF_FLUID_TYPE, SOURCE_C_STUFF, FLOWING_C_STUFF)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.C_STUFF_BLOCK)
            .bucket(ModFluids.C_STUFF_BUCKET);

    //////////////////////////////Cellulose Pulp\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_CELLULOSE_PULP = FLUIDS.register("source_cellulose_pulp",
            () -> new BaseFlowingFluid.Source(ModFluids.CELLULOSE_PULP_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_CELLULOSE_PULP = FLUIDS.register("flowing_cellulose_pulp",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CELLULOSE_PULP_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> CELLULOSE_PULP_BLOCK = ModBlocks.BLOCKS.register("cellulose_pulp_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_CELLULOSE_PULP.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> CELLULOSE_PULP_BUCKET = getBucket("cellulose_pulp_bucket", ModFluids.SOURCE_CELLULOSE_PULP);

    public static final BaseFlowingFluid.Properties CELLULOSE_PULP_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.CELLULOSE_PULP_FLUID_TYPE, SOURCE_CELLULOSE_PULP, FLOWING_CELLULOSE_PULP)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.CELLULOSE_PULP_BLOCK)
            .bucket(ModFluids.CELLULOSE_PULP_BUCKET);

    //////////////////////////////Molten Redstone\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_REDSTONE = FLUIDS.register("source_molten_redstone",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_REDSTONE_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_REDSTONE = FLUIDS.register("flowing_molten_redstone",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_REDSTONE_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_REDSTONE_BLOCK = ModBlocks.BLOCKS.register("molten_redstone_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_REDSTONE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .lightLevel(state -> 8).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_REDSTONE_BUCKET = getBucket("molten_redstone_bucket", ModFluids.SOURCE_MOLTEN_REDSTONE);

    public static final BaseFlowingFluid.Properties MOLTEN_REDSTONE_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_REDSTONE_FLUID_TYPE, SOURCE_MOLTEN_REDSTONE, FLOWING_MOLTEN_REDSTONE)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_REDSTONE_BLOCK)
            .bucket(ModFluids.MOLTEN_REDSTONE_BUCKET);

    //////////////////////////////Molten Quartz\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_QUARTZ = FLUIDS.register("source_molten_quartz",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_QUARTZ_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_QUARTZ = FLUIDS.register("flowing_molten_quartz",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_QUARTZ_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_QUARTZ_BLOCK = ModBlocks.BLOCKS.register("molten_quartz_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_QUARTZ.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_QUARTZ_BUCKET = getBucket("molten_quartz_bucket", ModFluids.SOURCE_MOLTEN_QUARTZ);

    public static final BaseFlowingFluid.Properties MOLTEN_QUARTZ_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_QUARTZ_FLUID_TYPE, SOURCE_MOLTEN_QUARTZ, FLOWING_MOLTEN_QUARTZ)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_QUARTZ_BLOCK)
            .bucket(ModFluids.MOLTEN_QUARTZ_BUCKET);

    //////////////////////////////Molten Glowstone\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_GLOWSTONE = FLUIDS.register("source_molten_glowstone",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_GLOWSTONE_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_GLOWSTONE = FLUIDS.register("flowing_molten_glowstone",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_GLOWSTONE_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_GLOWSTONE_BLOCK = ModBlocks.BLOCKS.register("molten_glowstone_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_GLOWSTONE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .lightLevel(state -> 10).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_GLOWSTONE_BUCKET = getBucket("molten_glowstone_bucket", ModFluids.SOURCE_MOLTEN_GLOWSTONE);

    public static final BaseFlowingFluid.Properties MOLTEN_GLOWSTONE_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_GLOWSTONE_FLUID_TYPE, SOURCE_MOLTEN_GLOWSTONE, FLOWING_MOLTEN_GLOWSTONE)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_GLOWSTONE_BLOCK)
            .bucket(ModFluids.MOLTEN_GLOWSTONE_BUCKET);

    //////////////////////////////Molten Amethyst\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_AMETHYST = FLUIDS.register("source_molten_amethyst",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_AMETHYST_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_AMETHYST = FLUIDS.register("flowing_molten_amethyst",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_AMETHYST_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_AMETHYST_BLOCK = ModBlocks.BLOCKS.register("molten_amethyst_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_AMETHYST.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_AMETHYST_BUCKET = getBucket("molten_amethyst_bucket", ModFluids.SOURCE_MOLTEN_AMETHYST);

    public static final BaseFlowingFluid.Properties MOLTEN_AMETHYST_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_AMETHYST_FLUID_TYPE, SOURCE_MOLTEN_AMETHYST, FLOWING_MOLTEN_AMETHYST)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_AMETHYST_BLOCK)
            .bucket(ModFluids.MOLTEN_AMETHYST_BUCKET);

    //////////////////////////////Molten Diamond\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_DIAMOND = FLUIDS.register("source_molten_diamond",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_DIAMOND_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_DIAMOND = FLUIDS.register("flowing_molten_diamond",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_DIAMOND_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_DIAMOND_BLOCK = ModBlocks.BLOCKS.register("molten_diamond_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_DIAMOND.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_DIAMOND_BUCKET = getBucket("molten_diamond_bucket", ModFluids.SOURCE_MOLTEN_DIAMOND);

    public static final BaseFlowingFluid.Properties MOLTEN_DIAMOND_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_DIAMOND_FLUID_TYPE, SOURCE_MOLTEN_DIAMOND, FLOWING_MOLTEN_DIAMOND)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_DIAMOND_BLOCK)
            .bucket(ModFluids.MOLTEN_DIAMOND_BUCKET);

    //////////////////////////////Molten Emerald\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_EMERALD = FLUIDS.register("source_molten_emerald",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_EMERALD_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_EMERALD = FLUIDS.register("flowing_molten_emerald",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_EMERALD_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_EMERALD_BLOCK = ModBlocks.BLOCKS.register("molten_emerald_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_EMERALD.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_EMERALD_BUCKET = getBucket("molten_emerald_bucket", ModFluids.SOURCE_MOLTEN_EMERALD);

    public static final BaseFlowingFluid.Properties MOLTEN_EMERALD_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_EMERALD_FLUID_TYPE, SOURCE_MOLTEN_EMERALD, FLOWING_MOLTEN_EMERALD)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_EMERALD_BLOCK)
            .bucket(ModFluids.MOLTEN_EMERALD_BUCKET);

    //////////////////////////////Molten Lapis\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_LAPIS = FLUIDS.register("source_molten_lapis",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_LAPIS_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_LAPIS = FLUIDS.register("flowing_molten_lapis",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_LAPIS_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_LAPIS_BLOCK = ModBlocks.BLOCKS.register("molten_lapis_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_LAPIS.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_LAPIS_BUCKET = getBucket("molten_lapis_bucket", ModFluids.SOURCE_MOLTEN_LAPIS);

    public static final BaseFlowingFluid.Properties MOLTEN_LAPIS_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_LAPIS_FLUID_TYPE, SOURCE_MOLTEN_LAPIS, FLOWING_MOLTEN_LAPIS)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_LAPIS_BLOCK)
            .bucket(ModFluids.MOLTEN_LAPIS_BUCKET);

    //////////////////////////////Dirty Molten Netherite\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_DIRTY_MOLTEN_NETHERITE = FLUIDS.register("source_dirty_molten_netherite",
            () -> new BaseFlowingFluid.Source(ModFluids.DIRTY_MOLTEN_NETHERITE_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_DIRTY_MOLTEN_NETHERITE = FLUIDS.register("flowing_dirty_molten_netherite",
            () -> new BaseFlowingFluid.Flowing(ModFluids.DIRTY_MOLTEN_NETHERITE_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> DIRTY_MOLTEN_NETHERITE_BLOCK = ModBlocks.BLOCKS.register("dirty_molten_netherite_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_DIRTY_MOLTEN_NETHERITE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> DIRTY_MOLTEN_NETHERITE_BUCKET = getBucket("dirty_molten_netherite_bucket", ModFluids.SOURCE_DIRTY_MOLTEN_NETHERITE);

    public static final BaseFlowingFluid.Properties DIRTY_MOLTEN_NETHERITE_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.DIRTY_MOLTEN_NETHERITE_FLUID_TYPE, SOURCE_DIRTY_MOLTEN_NETHERITE, FLOWING_DIRTY_MOLTEN_NETHERITE)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.DIRTY_MOLTEN_NETHERITE_BLOCK)
            .bucket(ModFluids.DIRTY_MOLTEN_NETHERITE_BUCKET);

    //////////////////////////////Molten Netherite\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_NETHERITE = FLUIDS.register("source_molten_netherite",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_NETHERITE_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_NETHERITE = FLUIDS.register("flowing_molten_netherite",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_NETHERITE_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_NETHERITE_BLOCK = ModBlocks.BLOCKS.register("molten_netherite_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_NETHERITE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_NETHERITE_BUCKET = getBucket("molten_netherite_bucket", ModFluids.SOURCE_MOLTEN_NETHERITE);

    public static final BaseFlowingFluid.Properties MOLTEN_NETHERITE_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_NETHERITE_FLUID_TYPE, SOURCE_MOLTEN_NETHERITE, FLOWING_MOLTEN_NETHERITE)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_NETHERITE_BLOCK)
            .bucket(ModFluids.MOLTEN_NETHERITE_BUCKET);

    //////////////////////////////Blaze Colloid\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_BLAZE_COLLOID = FLUIDS.register("source_blaze_colloid",
            () -> new BaseFlowingFluid.Source(ModFluids.BLAZE_COLLOID_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_BLAZE_COLLOID = FLUIDS.register("flowing_blaze_colloid",
            () -> new BaseFlowingFluid.Flowing(ModFluids.BLAZE_COLLOID_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> BLAZE_COLLOID_BLOCK = ModBlocks.BLOCKS.register("blaze_colloid_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_BLAZE_COLLOID.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .lightLevel(state -> 9).noLootTable()));

    public static final DeferredItem<Item> BLAZE_COLLOID_BUCKET = getBucket("blaze_colloid_bucket", ModFluids.SOURCE_BLAZE_COLLOID);

    public static final BaseFlowingFluid.Properties BLAZE_COLLOID_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.BLAZE_COLLOID_FLUID_TYPE, SOURCE_BLAZE_COLLOID, FLOWING_BLAZE_COLLOID)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.BLAZE_COLLOID_BLOCK)
            .bucket(ModFluids.BLAZE_COLLOID_BUCKET);

    //////////////////////////////Ghast Colloid\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_GHAST_COLLOID = FLUIDS.register("source_ghast_colloid",
            () -> new BaseFlowingFluid.Source(ModFluids.GHAST_COLLOID_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_GHAST_COLLOID = FLUIDS.register("flowing_ghast_colloid",
            () -> new BaseFlowingFluid.Flowing(ModFluids.GHAST_COLLOID_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> GHAST_COLLOID_BLOCK = ModBlocks.BLOCKS.register("ghast_colloid_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_GHAST_COLLOID.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> GHAST_COLLOID_BUCKET = getBucket("ghast_colloid_bucket", ModFluids.SOURCE_GHAST_COLLOID);

    public static final BaseFlowingFluid.Properties GHAST_COLLOID_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.GHAST_COLLOID_FLUID_TYPE, SOURCE_GHAST_COLLOID, FLOWING_GHAST_COLLOID)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.GHAST_COLLOID_BLOCK)
            .bucket(ModFluids.GHAST_COLLOID_BUCKET);

    //////////////////////////////Wither Colloid\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_WITHER_COLLOID = FLUIDS.register("source_wither_colloid",
            () -> new BaseFlowingFluid.Source(ModFluids.WITHER_COLLOID_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_WITHER_COLLOID = FLUIDS.register("flowing_wither_colloid",
            () -> new BaseFlowingFluid.Flowing(ModFluids.WITHER_COLLOID_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> WITHER_COLLOID_BLOCK = ModBlocks.BLOCKS.register("wither_colloid_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_WITHER_COLLOID.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> WITHER_COLLOID_BUCKET = getBucket("wither_colloid_bucket", ModFluids.SOURCE_WITHER_COLLOID);

    public static final BaseFlowingFluid.Properties WITHER_COLLOID_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.WITHER_COLLOID_FLUID_TYPE, SOURCE_WITHER_COLLOID, FLOWING_WITHER_COLLOID)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.WITHER_COLLOID_BLOCK)
            .bucket(ModFluids.WITHER_COLLOID_BUCKET);

    //////////////////////////////Ender Colloid\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_ENDER_COLLOID = FLUIDS.register("source_ender_colloid",
            () -> new BaseFlowingFluid.Source(ModFluids.ENDER_COLLOID_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_ENDER_COLLOID = FLUIDS.register("flowing_ender_colloid",
            () -> new BaseFlowingFluid.Flowing(ModFluids.ENDER_COLLOID_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> ENDER_COLLOID_BLOCK = ModBlocks.BLOCKS.register("ender_colloid_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_ENDER_COLLOID.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .lightLevel(state -> 4).noLootTable()));

    public static final DeferredItem<Item> ENDER_COLLOID_BUCKET = getBucket("ender_colloid_bucket", ModFluids.SOURCE_ENDER_COLLOID);

    public static final BaseFlowingFluid.Properties ENDER_COLLOID_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.ENDER_COLLOID_FLUID_TYPE, SOURCE_ENDER_COLLOID, FLOWING_ENDER_COLLOID)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.ENDER_COLLOID_BLOCK)
            .bucket(ModFluids.ENDER_COLLOID_BUCKET);

    //////////////////////////////Molten Soul Silica\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_MOLTEN_SOUL_SILICA = FLUIDS.register("source_molten_soul_silica",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_SOUL_SILICA_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_MOLTEN_SOUL_SILICA = FLUIDS.register("flowing_molten_soul_silica",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_SOUL_SILICA_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> MOLTEN_SOUL_SILICA_BLOCK = ModBlocks.BLOCKS.register("molten_soul_silica_block",
            () -> new ModLiquidBlock(ModFluids.SOURCE_MOLTEN_SOUL_SILICA.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> MOLTEN_SOUL_SILICA_BUCKET = getBucket("molten_soul_silica_bucket", ModFluids.SOURCE_MOLTEN_SOUL_SILICA);

    public static final BaseFlowingFluid.Properties MOLTEN_SOUL_SILICA_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.MOLTEN_SOUL_SILICA_FLUID_TYPE, SOURCE_MOLTEN_SOUL_SILICA, FLOWING_MOLTEN_SOUL_SILICA)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.MOLTEN_SOUL_SILICA_BLOCK)
            .bucket(ModFluids.MOLTEN_SOUL_SILICA_BUCKET);

    //////////////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }

    private static DeferredItem<Item> getBucket(String name, Supplier<FlowingFluid> fluid) {
        return ModItems.ITEMS.registerItem(name, properties -> new BucketItem(fluid.get(), properties
                .craftRemainder(Items.BUCKET).stacksTo(1)));
    }
}
