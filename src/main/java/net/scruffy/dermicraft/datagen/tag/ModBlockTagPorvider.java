package net.scruffy.dermicraft.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagPorvider extends BlockTagsProvider {
    public ModBlockTagPorvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Dermicraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(Tags.Blocks.GLASS_BLOCKS_CHEAP)
                .add(ModBlocks.CALCIUM_GLASS.get())

        ;

        tag(ModTags.Blocks.COLLECTIBLE)
                .addTag(ModTags.Blocks.TUMORS)
                .add(ModBlocks.DROOLING_CAULDRON.get())
                .add(ModBlocks.DROOLING_GEODE.get())
                .add(ModBlocks.OUTERFACE.get())
                .add(ModBlocks.SKIN_TANK.get())
                .add(ModBlocks.CHARRED_TANK.get())
                .add(ModBlocks.KNOWLEDGE_VAT.get())
                .add(ModBlocks.MASTICATOR.get())
                .add(ModBlocks.CHARRED_MASTICATOR.get())
                .add(ModBlocks.EFFLUENTCER.get())
                .add(ModBlocks.CHARRED_EFFLUENTCER.get())
                .add(ModBlocks.METASTASIZER.get())
                .add(ModBlocks.CHARRED_METASTASIZER.get())
                .add(ModBlocks.MUTATOR.get())
                .add(ModBlocks.CHARRED_MUTATOR.get())
                .add(ModBlocks.RENDER_FURNACE.get())
                .add(ModBlocks.GRAFTING_TABLE.get())
                .add(ModBlocks.RENDER_KILN.get())
                .add(ModBlocks.CHARRED_RENDER_KILN.get())
                .add(ModBlocks.CRAW.get())
                .add(ModBlocks.CHARRED_CRAW.get())
                .add(ModBlocks.INNARDS_DUCT.get())
                .add(ModBlocks.CHARRED_INNARDS_DUCT.get())
                .add(ModBlocks.INNARDS_NODE.get())
                .add(ModBlocks.CHARRED_INNARDS_NODE.get())
                .add(ModBlocks.INNARDS_GATE_CONTROLLER.get())
                .add(ModBlocks.INNARDS_GATE_BUFFER.get())
                .add(ModBlocks.INNARDS_GATE_PORT.get())
                .add(ModBlocks.MR_FARMER.get())
                .add(ModBlocks.MR_SHEPARD.get())
                .add(ModBlocks.STONE_LAB_FLOOR.get())
                .add(ModBlocks.COBBLESTONE_LAB_FLOOR.get())
                .add(ModBlocks.DEEPSLATE_LAB_FLOOR.get())
                .add(ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR.get())
                .add(ModBlocks.DIORITE_LAB_FLOOR.get())
                .add(ModBlocks.ANDESITE_LAB_FLOOR.get())
                .add(ModBlocks.GRANITE_LAB_FLOOR.get())
                // Bottom half only -- the top is a purely visual companion with no item of its own
                // worth recovering; WorkbenchBlock's own onRemove already takes it with the bottom.
                .add(ModBlocks.WORKBENCH.get())
        ;

        tag(ModTags.Blocks.LAB_FLOOR)
                .add(ModBlocks.STONE_LAB_FLOOR.get())
                .add(ModBlocks.COBBLESTONE_LAB_FLOOR.get())
                .add(ModBlocks.DEEPSLATE_LAB_FLOOR.get())
                .add(ModBlocks.COBBLED_DEEPSLATE_LAB_FLOOR.get())
                .add(ModBlocks.DIORITE_LAB_FLOOR.get())
                .add(ModBlocks.ANDESITE_LAB_FLOOR.get())
                .add(ModBlocks.GRANITE_LAB_FLOOR.get())
        ;

        tag(ModTags.Blocks.AGGREGATE)
                .add(Blocks.GRASS_BLOCK)
                .add(Blocks.DIRT)
                .add(Blocks.COARSE_DIRT)
                .add(Blocks.PODZOL)
                .add(Blocks.MYCELIUM)
                .add(Blocks.ROOTED_DIRT)
                .add(Blocks.SAND)
                .add(Blocks.RED_SAND)
                .add(Blocks.GRAVEL)
                .add(Blocks.CLAY)
                .add(Blocks.SNOW)
                .add(Blocks.SNOW_BLOCK)
                .add(Blocks.NETHERRACK)
        ;

        tag(ModTags.Blocks.AGGREGATE_HOT)
                .add(Blocks.MAGMA_BLOCK)
        ;

        tag(ModTags.Blocks.AGGREGATE_METAPHYSICAL)
                .add(Blocks.SOUL_SAND)
                .add(Blocks.SOUL_SOIL)
        ;

        tag(ModTags.Blocks.STONE_ORE)
                .add(Blocks.STONE)
                .add(Blocks.COBBLESTONE)
                .add(Blocks.DEEPSLATE)
                .add(Blocks.COBBLED_DEEPSLATE)
                .add(Blocks.GRANITE)
                .add(Blocks.DIORITE)
                .add(Blocks.ANDESITE)
                .add(Blocks.TUFF)
                .add(Blocks.CALCITE)
                .add(Blocks.OBSIDIAN)
                .add(Blocks.POINTED_DRIPSTONE)
                .addTag(BlockTags.COAL_ORES)
                .addTag(BlockTags.COPPER_ORES)
                .addTag(BlockTags.IRON_ORES)
                .addTag(BlockTags.GOLD_ORES)
                .addTag(BlockTags.REDSTONE_ORES)
                .addTag(BlockTags.LAPIS_ORES)
                .addTag(BlockTags.DIAMOND_ORES)
                .addTag(BlockTags.EMERALD_ORES)
        ;

        tag(ModTags.Blocks.EXTRACTABLE)
                .add(ModBlocks.INERT_TUMOR.get())
                .add(ModBlocks.EYE_TUMOR.get())
                .add(ModBlocks.MUSCLE_TUMOR.get())
                .add(ModBlocks.NERVE_TUMOR.get())
        ;

        tag(ModTags.Blocks.HAS_SCREEN)
                .add(ModBlocks.DROOLING_CAULDRON.get())
                .add(ModBlocks.DROOLING_GEODE.get())
                .add(ModBlocks.MASTICATOR.get())
                .add(ModBlocks.CHARRED_MASTICATOR.get())
                .add(ModBlocks.SKIN_TANK.get())
                .add(ModBlocks.CHARRED_TANK.get())
                .add(ModBlocks.EFFLUENTCER.get())
                .add(ModBlocks.CHARRED_EFFLUENTCER.get())
                .add(ModBlocks.METASTASIZER.get())
                .add(ModBlocks.CHARRED_METASTASIZER.get())
                .add(ModBlocks.MUTATOR.get())
                .add(ModBlocks.CHARRED_MUTATOR.get())
                .add(ModBlocks.RENDER_FURNACE.get())
                .add(ModBlocks.GRAFTING_TABLE.get())
                .add(ModBlocks.RENDER_KILN.get())
                .add(ModBlocks.CHARRED_RENDER_KILN.get())
                .add(ModBlocks.CRAW.get())
                .add(ModBlocks.CHARRED_CRAW.get())
                .add(ModBlocks.MR_FARMER.get())
                .add(ModBlocks.MR_SHEPARD.get())
        ;

        tag(ModTags.Blocks.INJECTABLE)
                .add(ModBlocks.STITCHED_TUMOR.get())

        ;

        tag(ModTags.Blocks.SUTABLE)
                .add(ModBlocks.MARRED_TUMOR.get())

        ;

        tag(ModTags.Blocks.DIPPING_TANKS)
                .add(ModBlocks.BEAKER.get())
                .add(ModBlocks.DROOLING_CAULDRON.get())
                .add(ModBlocks.DROOLING_GEODE.get())
                .add(ModBlocks.MASTICATOR.get())
                .add(ModBlocks.CHARRED_MASTICATOR.get())
                .add(ModBlocks.SKIN_TANK.get())
                .add(ModBlocks.CHARRED_TANK.get())
                .add(ModBlocks.EFFLUENTCER.get())
                .add(ModBlocks.CHARRED_EFFLUENTCER.get())
                .add(ModBlocks.KNOWLEDGE_VAT.get())
        ;

        tag(ModTags.Blocks.TUMORS)
                .add(ModBlocks.INERT_TUMOR.get())
                .add(ModBlocks.MARRED_TUMOR.get())
                .add(ModBlocks.STITCHED_TUMOR.get())

                .add(ModBlocks.EYE_TUMOR.get())
                .add(ModBlocks.MUSCLE_TUMOR.get())
                .add(ModBlocks.NERVE_TUMOR.get())

        ;
    }
}
