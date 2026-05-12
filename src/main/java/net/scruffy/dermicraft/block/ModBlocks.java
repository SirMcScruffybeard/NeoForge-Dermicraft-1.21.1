package net.scruffy.dermicraft.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.entity.custom.tumor.InertTumorBlock;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Dermicraft.MOD_ID);
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
    public static final DeferredBlock<Block> INERT_TUMOR = registerBlock("inert_tumor",
            () -> new InertTumorBlock(BlockBehaviour.Properties.of()));


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
