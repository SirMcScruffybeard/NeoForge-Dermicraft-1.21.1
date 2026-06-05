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

    //////////////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }

    private static DeferredItem<Item> getBucket(String name, Supplier<FlowingFluid> fluid) {
        return ModItems.ITEMS.registerItem(name, properties -> new BucketItem(fluid.get(), properties
                .craftRemainder(Items.BUCKET).stacksTo(1)));
    }
}
