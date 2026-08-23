package net.scruffy.dermicraft.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.custom.*;
import net.scruffy.dermicraft.block.custom.duct.InnardsDuctBlock;
import net.scruffy.dermicraft.block.custom.floor.LabFloorBlock;
import net.scruffy.dermicraft.block.custom.duct.NodeBlock;
import net.scruffy.dermicraft.block.custom.gate.GateBufferBlock;
import net.scruffy.dermicraft.block.custom.gate.GateControllerBlock;
import net.scruffy.dermicraft.block.custom.gate.GatePortBlock;
import net.scruffy.dermicraft.block.custom.tumor.*;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BeakerItem;
import net.scruffy.dermicraft.item.custom.SkinTankBlockItem;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Dermicraft.MOD_ID);
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
    // Eye/Muscle/Nerve/Inert Tumor share one TumorBlock class -- the only difference between them
    // is which loot table their registry name resolves to (see ModBlockLootTableProvider).
    public static final DeferredBlock<Block> INERT_TUMOR = registerBlock("inert_tumor", () -> new TumorBlock(tumorProperties()));
    public static final DeferredBlock<Block> MARRED_TUMOR = registerBlock("marred_tumor",
            () -> new MarredTumorBlock(BlockBehaviour.Properties.of()));
    public static final DeferredBlock<Block> STITCHED_TUMOR = registerBlock("stitched_tumor",
            () -> new StitchedTumorBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> EYE_TUMOR = registerBlock("eye_tumor", () -> new TumorBlock(tumorProperties()));
    public static final DeferredBlock<Block> MUSCLE_TUMOR = registerBlock("muscle_tumor", () -> new TumorBlock(tumorProperties()));
    public static final DeferredBlock<Block> NERVE_TUMOR = registerBlock("nerve_tumor", () -> new TumorBlock(tumorProperties()));

    private static BlockBehaviour.Properties tumorProperties() {
        return BlockBehaviour.Properties.of()
                .strength(.05f)
                .explosionResistance(15f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.6f)
                .ignitedByLava();
    }

    // Charred Tumor -- a plain Block (no TumorBlock harvest/inject/suture mechanics), paired with
    // the new charred-flesh texture. Slightly tougher than Inert Tumor's own strength, since it's
    // meant to read as flesh that's been hardened by heat rather than the soft raw kind. No
    // ignitedByLava -- already charred, re-igniting it doesn't fit.
    public static final DeferredBlock<Block> CHARRED_TUMOR = registerBlock("charred_tumor",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(.35f)
                    .explosionResistance(15f)
                    .sound(SoundType.SLIME_BLOCK)
                    .friction(0.6f)));

    // Hot Bone -- a vanilla MagmaBlock subclass (not a custom class), reusing its exact
    // walk-on-damage behavior wholesale rather than reimplementing it. Slightly weaker than vanilla
    // Bone Block's 2.0 strength. Emissive via lightLevel, same technique as the Molten fluid family
    // (whole-block glow, not a per-pixel emissive texture layer) -- see MagmaBlock's own vanilla
    // light level (3) for precedent; bumped higher here since this is meant to read as hotter/more
    // "actively molten" than plain magma.
    public static final DeferredBlock<Block> HOT_BONE = registerBlock("hot_bone",
            () -> new MagmaBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.BONE_BLOCK)
                    .lightLevel(state -> 6)));

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

    // Matches vanilla Cauldron's own hardness/blast resistance (strength 2.0, pickaxe required) --
    // was bare Properties.of() (hardness 0) despite being modeled on it.
    public static final DeferredBlock<Block> DROOLING_CAULDRON = registerBlock("drooling_cauldron",
            () -> new DroolingCauldronBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    // Cauldron-shaped model (walls + open fluid-pool cavity), not a full cube --
                    // without this, neighboring blocks' touching faces get culled as if this were
                    // opaque, leaving the block behind visibly see-through. Matches vanilla's own
                    // Cauldron, which sets the same flag for the same reason.
                    .noOcclusion()));

    // Tier 2 -- standalone for now (evolution FROM Drooling Cauldron isn't built yet, see
    // dermicraft-machine-notes.md's Drooling Cauldron entry). Same strength/tool requirement as
    // Cauldron. No lightLevel() call here -- DroolingMachineBlock's own constructor sets it
    // dynamically off LIGHT_LEVEL (kept in sync with the tank's actual contents), which would
    // silently override a fixed value set here anyway.
    public static final DeferredBlock<Block> DROOLING_CRUCIBLE = registerBlock("drooling_crucible",
            () -> new DroolingCrucibleBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())); // same cauldron-shaped-model reasoning as Drooling Cauldron above

    // Workbench bottom half -- keeps the registry id "workbench" (established before the top half
    // existed) and hosts the real Storage/Mod/Fabrication GUI (see WorkbenchBlock's own javadoc).
    // noOcclusion -- GeckoLib-rendered, doesn't fill the full cube, so a neighbor's face shouldn't
    // get culled against it (same reasoning as Brain's own noOcclusion).
    public static final DeferredBlock<Block> WORKBENCH = registerBlock("workbench",
            () -> new WorkbenchBlock(BlockBehaviour.Properties.of()
                    // Furnace-tier -- sturdier than a crafting table's default 2.5, since this
                    // carries persistent Storage/Mod-page state a player shouldn't lose to an
                    // accidental break.
                    .strength(3.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // Workbench top half -- purely visual/animated this pass, see WorkbenchTopBlock's own javadoc.
    public static final DeferredBlock<Block> WORKBENCH_TOP = registerBlock("workbench_top",
            () -> new WorkbenchTopBlock(BlockBehaviour.Properties.of()
                    // Furnace-tier -- sturdier than a crafting table's default 2.5, since this
                    // carries persistent Storage/Mod-page state a player shouldn't lose to an
                    // accidental break.
                    .strength(3.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // Brain Block: Gear Stations' construction-defining "smart" ingredient (see
    // dermicraft-gear-stations-notes.md -> Construction); also stands alone as a decoration
    // block, same "every item needs more than one use" convention as Proto Brain's own item.
    public static final DeferredBlock<Block> BRAIN = registerBlock("brain",
            () -> new BrainBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion())); // cutout render layer -- don't hide neighbouring faces behind its transparent regions

    public static final DeferredBlock<Block> OUTERFACE = registerBlock("outerface", OuterfaceBlock::new);

    // Furnace-tier hardness/blast resistance -- a bare Properties.of() (hardness 0, one-hit-with-
    // fists, no tool required) was far too fragile for a real machine. Applied mod-wide across
    // every machine block below (Drooling Cauldron deliberately excepted -- modeled on a vanilla
    // Cauldron's own fragility).
    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<Block> MASTICATOR = registerBlock("masticator",
            () -> new  MasticatorBlock(machineProperties()));

    // Charred Masticator -- Masticator's hazard-gated Tier 2 evolution, see
    // CharredMasticatorBlockEntity for the actual capability leap (thermal-tolerant tanks).
    public static final DeferredBlock<Block> CHARRED_MASTICATOR = registerBlock("charred_masticator",
            () -> new CharredMasticatorBlock(machineProperties()));

    // Registered manually (not via registerBlock's auto-generated plain BlockItem) -- Skin Tank
    // preserves its contents on Forceps pickup (see IPreserveContentsOnPickup/ICollectBlocks), so
    // its item needs SkinTankBlockItem's shift-hidden fluid tooltip to actually show what's inside.
    public static final DeferredBlock<Block> SKIN_TANK = BLOCKS.register("skin_tank",
            () -> new SkinTankBlock(machineProperties()));
    public static final DeferredItem<Item> SKIN_TANK_ITEM = ModItems.ITEMS.register("skin_tank",
            () -> new SkinTankBlockItem(SKIN_TANK.get(), new Item.Properties()));

    public static final DeferredBlock<Block> EFFLUENTCER = registerBlock("effluentcer",
            () -> new EffluentcerBlock(machineProperties()));

    public static final DeferredBlock<Block> METASTASIZER = registerBlock("metastasizer",
            () -> new MetastasizerBlock(machineProperties()));

    public static final DeferredBlock<Block> MUTATOR = registerBlock("mutator",
            () -> new MutatorBlock(machineProperties()));

    public static final DeferredBlock<Block> RENDER_FURNACE = registerBlock("render_furnace",
            () -> new RenderFurnaceBlock(machineProperties()));

    public static final DeferredBlock<Block> GRAFTING_TABLE = registerBlock("grafting_table",
            () -> new GraftingTableBlock(machineProperties()));

    public static final DeferredBlock<Block> RENDER_KILN = registerBlock("render_kiln",
            () -> new RenderKilnBlock(machineProperties()));

    public static final DeferredBlock<Block> CRAW = registerBlock("craw",
            () -> new CrawBlock(machineProperties()));

    public static final DeferredBlock<Block> MR_FARMER = registerBlock("mr_farmer",
            () -> new MrFarmerBlock(machineProperties()));

    public static final DeferredBlock<Block> MR_SHEPARD = registerBlock("mr_shepard",
            () -> new net.scruffy.dermicraft.block.custom.MrShepardBlock(machineProperties()));

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

    ////////////////////Flesh Lab Floor\\\\\\\\\\\\\\\\\\\\
    // All seven current variants are the Tier 1 crafting-table bootstrap set (dermicraft-machine-
    // notes.md -> Lab Floor -> Floor Tiers) -- safe fluids only, adjacent-only reach. Higher-tier
    // (metal) variants are still design-only; when they land they just pass a higher tier int here.
    public static final DeferredBlock<Block> STONE_LAB_FLOOR = registerBlock("stone_lab_floor",
            () -> new LabFloorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE), 1));
    public static final DeferredBlock<Block> COBBLESTONE_LAB_FLOOR = registerBlock("cobblestone_lab_floor",
            () -> new LabFloorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE), 1));
    public static final DeferredBlock<Block> DEEPSLATE_LAB_FLOOR = registerBlock("deepslate_lab_floor",
            () -> new LabFloorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE), 1));
    public static final DeferredBlock<Block> COBBLED_DEEPSLATE_LAB_FLOOR = registerBlock("cobbled_deepslate_lab_floor",
            () -> new LabFloorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE), 1));
    public static final DeferredBlock<Block> DIORITE_LAB_FLOOR = registerBlock("diorite_lab_floor",
            () -> new LabFloorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIORITE), 1));
    public static final DeferredBlock<Block> ANDESITE_LAB_FLOOR = registerBlock("andesite_lab_floor",
            () -> new LabFloorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ANDESITE), 1));
    public static final DeferredBlock<Block> GRANITE_LAB_FLOOR = registerBlock("granite_lab_floor",
            () -> new LabFloorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRANITE), 1));

    public static final DeferredBlock<Block> BEAKER = BLOCKS.register("beaker",
            () -> new BeakerBlock(BlockBehaviour.Properties.of()));
    public static final DeferredItem<Item> BEAKER_ITEM = ModItems.ITEMS.register("beaker",
            () -> new BeakerItem(BEAKER.get(), new Item.Properties()));


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
