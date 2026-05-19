package net.scruffy.dermicraft.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.custom.SkinTankBlock;
import net.scruffy.dermicraft.block.custom.tumor.*;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Dermicraft.MOD_ID);
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
    public static final DeferredBlock<Block> INERT_TUMOR = registerBlock("inert_tumor", InertTumorBlock::new);
    public static final DeferredBlock<Block> MARRED_TUMOR = registerBlock("marred_tumor",
            () -> new MarredTumorBlock(BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> STITCHED_TUMOR = registerBlock("stitched_tumor",
            () -> new StitchedTumorBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> EYE_TUMOR = registerBlock("eye_tumor", EyeTumorBlock::new);
    public static final DeferredBlock<Block> MUSCLE_TUMOR = registerBlock("muscle_tumor", MuscleTumorBlock::new);
    public static final DeferredBlock<Block> NERVE_TUMOR = registerBlock("nerve_tumor", NerveTumorBlock::new);

    public static final DeferredBlock<Block> SKIN_TANK = registerBlock("skin_tank",
            () -> new SkinTankBlock(BlockBehaviour.Properties.of()));


    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
