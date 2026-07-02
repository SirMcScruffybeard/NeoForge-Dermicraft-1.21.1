package net.scruffy.dermicraft.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
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
                .add(ModBlocks.OUTERFACE.get())
                .add(ModBlocks.SKIN_TANK.get())
                .add(ModBlocks.MASTICATOR.get())
                .add(ModBlocks.EFFLUENTCER.get())
        ;

        tag(ModTags.Blocks.EXTRACTABLE)
                .add(ModBlocks.INERT_TUMOR.get())
                .add(ModBlocks.EYE_TUMOR.get())
                .add(ModBlocks.MUSCLE_TUMOR.get())
                .add(ModBlocks.NERVE_TUMOR.get())
        ;

        tag(ModTags.Blocks.HAS_SCREEN)
                .add(ModBlocks.DROOLING_CAULDRON.get())
                .add(ModBlocks.MASTICATOR.get())
                .add(ModBlocks.SKIN_TANK.get())
                .add(ModBlocks.EFFLUENTCER.get())
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
                .add(ModBlocks.MASTICATOR.get())
                .add(ModBlocks.SKIN_TANK.get())
                .add(ModBlocks.EFFLUENTCER.get())
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
