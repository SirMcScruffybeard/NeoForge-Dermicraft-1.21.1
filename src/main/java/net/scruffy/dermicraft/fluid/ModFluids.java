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


    //////////////////////////////Nutrient Slurry\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static final Supplier<FlowingFluid> SOURCE_NUTRIENT_SLURRY = FLUIDS.register("source_nutrient_slurry",
            () -> new BaseFlowingFluid.Source(ModFluids.NUTRIENT_SLURRY_PROPERTIES));

    public static final Supplier<FlowingFluid> FLOWING_NUTRIENT_SLURRY = FLUIDS.register("flowing_nutrient_slurry",
            () -> new BaseFlowingFluid.Flowing(ModFluids.NUTRIENT_SLURRY_PROPERTIES));

    public static final DeferredBlock<LiquidBlock> NUTRIENT_SLURRY_BLOCK = ModBlocks.BLOCKS.register("nutrient_slurry_block",
            () -> new LiquidBlock(ModFluids.SOURCE_NUTRIENT_SLURRY.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static final DeferredItem<Item> NUTRIENT_SLURRY_BUCKET = getBucket("nutrient_slurry_bucket", ModFluids.SOURCE_NUTRIENT_SLURRY);

    public static final BaseFlowingFluid.Properties NUTRIENT_SLURRY_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.NUTRIENT_SLURRY_FLUID_TYPE, SOURCE_NUTRIENT_SLURRY, FLOWING_NUTRIENT_SLURRY)
            .slopeFindDistance(2).levelDecreasePerBlock(1)
            .block(ModFluids.NUTRIENT_SLURRY_BLOCK)
            .bucket(ModFluids.NUTRIENT_SLURRY_BUCKET);

    //////////////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }

    private static DeferredItem<Item> getBucket(String name, Supplier<FlowingFluid> fluid) {
        return ModItems.ITEMS.registerItem(name, properties -> new BucketItem(fluid.get(), properties
                .craftRemainder(Items.BUCKET).stacksTo(1)));
    }
}
