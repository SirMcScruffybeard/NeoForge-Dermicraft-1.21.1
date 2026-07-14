package net.scruffy.dermicraft.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.custom.*;
import net.scruffy.dermicraft.block.custom.duct.InnardsDuctBlock;
import net.scruffy.dermicraft.block.custom.duct.NodeBlock;
import net.scruffy.dermicraft.block.custom.gate.GateBufferBlock;
import net.scruffy.dermicraft.block.custom.gate.GateControllerBlock;
import net.scruffy.dermicraft.block.custom.gate.GatePortBlock;
import net.scruffy.dermicraft.block.custom.tumor.*;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BeakerItem;
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

    public static final DeferredBlock<Block> CALCIUM_GLASS = registerBlock("calcium_glass",
            () -> new ModGlassBlock(BlockBehaviour.Properties.of()
                    .instrument(NoteBlockInstrument.HAT) // Makes hat sound on note blocks
                    .sound(SoundType.GLASS)             // Glass breaking/walking sounds
                    .strength(0.3F)                     // Instantly breakable with fist, like vanilla
                    .noOcclusion()                      // Allows rendering behind the block
                    .isValidSpawn((state, getter, pos, entityType) -> false) // Prevents mobs spawning on it
                    .isRedstoneConductor((state, getter, pos) -> false)      // Redstone doesn't pass through
                    .isSuffocating((state, getter, pos) -> false)           // Prevents suffocation damage
                    .isViewBlocking((state, getter, pos) -> false)          // Allows line-of-sight (e.g., endermen)
            ));

    public static final DeferredBlock<Block> DROOLING_CAULDRON = registerBlock("drooling_cauldron",
            () -> new DroolingCauldronBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> OUTERFACE = registerBlock("outerface", OuterfaceBlock::new);

    public static final DeferredBlock<Block> MASTICATOR = registerBlock("masticator",
            () -> new  MasticatorBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> SKIN_TANK = registerBlock("skin_tank",
            () -> new SkinTankBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> EFFLUENTCER = registerBlock("effluentcer",
            () -> new EffluentcerBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> METASTASIZER = registerBlock("metastasizer",
            () -> new MetastasizerBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> CRAW = registerBlock("craw",
            () -> new CrawBlock(BlockBehaviour.Properties.of()));

    // Ducts and Nodes are the exception to the "destroyed on normal break" rule: they drop
    // themselves (see ModBlockLootTableProvider) so players aren't punished for pipe re-fiddling.
    // They still keep the mod-wide Forceps pickup via the COLLECTIBLE tag.
    public static final DeferredBlock<Block> INNARDS_DUCT = registerBlock("innards_duct",
            () -> new InnardsDuctBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .sound(SoundType.HONEY_BLOCK)
                    .noOcclusion()));

    public static final DeferredBlock<Block> INNARDS_DUCT_ARM = registerBlock("innards_duct_arm",
            () -> new Block(BlockBehaviour.Properties.of().noOcclusion().noLootTable()));

    public static final DeferredBlock<Block> INNARDS_DUCT_END = registerBlock("innards_duct_end",
            () -> new Block(BlockBehaviour.Properties.of().noOcclusion().noLootTable()));

    public static final DeferredBlock<Block> INNARDS_NODE = registerBlock("innards_node",
            () -> new NodeBlock(BlockBehaviour.Properties.of()
                    .strength(0.8f)
                    .sound(SoundType.HONEY_BLOCK)));

    ////////////////////Innards Gate (channel automation multiblock)\\\\\\\\\\\\\\\\\\\\
    public static final DeferredBlock<Block> INNARDS_GATE_CONTROLLER = registerBlock("innards_gate_controller",
            () -> new GateControllerBlock(BlockBehaviour.Properties.of()
                    .strength(0.8f)
                    .sound(SoundType.HONEY_BLOCK)));

    public static final DeferredBlock<Block> INNARDS_GATE_BUFFER = registerBlock("innards_gate_buffer",
            () -> new GateBufferBlock(BlockBehaviour.Properties.of()
                    .strength(0.8f)
                    .sound(SoundType.HONEY_BLOCK)));

    public static final DeferredBlock<Block> INNARDS_GATE_PORT = registerBlock("innards_gate_port",
            () -> new GatePortBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .sound(SoundType.HONEY_BLOCK)
                    .noOcclusion()));

    public static final DeferredBlock<Block> BEAKER = BLOCKS.register("beaker",
            () -> new BeakerBlock(BlockBehaviour.Properties.of()));
    public static final DeferredItem<Item> BEAKER_ITEM = ModItems.ITEMS.register("beaker",
            () -> new BeakerItem(BEAKER.get(), new Item.Properties().stacksTo(1)));


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
