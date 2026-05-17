package net.scruffy.dermicraft.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Dermicraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(ModTags.Items.HARVEST_TOOLS)
                .add(ModItems.SCALPEL.get())
        ;

        tag(ModTags.Items.SUTURE_TOOLS)
                .add(ModItems.SUTURE_KIT.get());

        tag(ModTags.Items.ANIMAL_MEATS)
                .addTag(Tags.Items.FOODS_RAW_MEAT)
                .add(Items.ROTTEN_FLESH)
        ;

        tag(ModTags.Items.PART_ITEMS)
                .add(ModItems.EYE.get())
                .add(ModItems.DENSE_MUSCLE.get())
                .add(ModItems.NERVE_CLUSTER.get())
        ;

        tag(ModTags.Items.BIOFUELS)
                .add(ModFluids.NUTRIENT_SLURRY_BUCKET.get())

                ;

    }
}
