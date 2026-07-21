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

        tag(ModTags.Items.COLLECTION_TOOLS)
                .add(ModItems.FORCEPS.get())

        ;

        tag(ModTags.Items.EXTRACTION_TOOLS)
                .add(ModItems.SCALPEL.get())
        ;

        tag(ModTags.Items.INJECTION_TOOLS)
                .add(ModItems.SYRINGE.get())

        ;

        tag(ModTags.Items.SUTURE_TOOLS)
                .add(ModItems.SUTURE_KIT.get());


        tag(ModTags.Items.ANIMAL_MEATS)
                .addTag(Tags.Items.FOODS_RAW_MEAT)
                .add(Items.ROTTEN_FLESH)
        ;

        tag(ModTags.Items.PLANT_FOOD)
                .addTag(Tags.Items.FOODS_BREAD)
                .addTag(Tags.Items.FOODS_FRUIT)
                .addTag(Tags.Items.FOODS_BERRY)
                .addTag(Tags.Items.FOODS_VEGETABLE)
        ;

        tag(ModTags.Items.MEAT_FOOD)
                .addTag(ModTags.Items.ANIMAL_MEATS)
                .addTag(Tags.Items.FOODS_COOKED_MEAT)
        ;

        tag(ModTags.Items.PART_ITEMS)
                .add(ModItems.EYE.get())
                .add(ModItems.DENSE_MUSCLE.get())
                .add(ModItems.NERVE_CLUSTER.get())
        ;

        tag(ModTags.Items.BIOFUELS)
                .add(ModFluids.CRUDE_SLURRY_BUCKET.get())
                .add(ModFluids.CONCENTRATED_SLURRY_BUCKET.get())

        ;

        tag(ModTags.Items.STONE_BLEND_ROSTER)
                .add(Items.STONE)
                .add(Items.COBBLESTONE)
                .add(Items.ANDESITE)
                .add(Items.DIORITE)
                .add(Items.GRANITE)
                .add(Items.DEEPSLATE)
                .add(Items.COBBLED_DEEPSLATE)
                .add(Items.CALCITE)
                .add(Items.TUFF)
                .add(Items.GRAVEL)
                .add(Items.DRIPSTONE_BLOCK)
                .add(Items.POINTED_DRIPSTONE)
        ;

        tag(ModTags.Items.SILICA_BLEND_ROSTER)
                .add(Items.SAND)
                .add(Items.RED_SAND)
                .add(Items.SANDSTONE)
                .add(Items.RED_SANDSTONE)
        ;

        tag(ModTags.Items.CLAY_BLEND_ROSTER)
                .add(Items.CLAY)
        ;

        tag(ModTags.Items.SILICA_BLEND_RECYCLING)
                .add(Items.GLASS)
        ;

        // Brick, Bricks, and Flower Pot deliberately excluded -- unlike the rest of this tag (flat
        // 1000 mB regardless of item), they get individually-priced Masticator recipes mirroring
        // their own Metastasizer duplication costs (see ModRecipeProvider), so they can't also sit
        // in this flat-rate tag without a duplicate/ambiguous-recipe conflict.
        tag(ModTags.Items.CLAY_BLEND_RECYCLING)
                .add(Items.CLAY_BALL)
                .add(Items.TERRACOTTA)
                .add(Items.WHITE_TERRACOTTA)
                .add(Items.ORANGE_TERRACOTTA)
                .add(Items.MAGENTA_TERRACOTTA)
                .add(Items.LIGHT_BLUE_TERRACOTTA)
                .add(Items.YELLOW_TERRACOTTA)
                .add(Items.LIME_TERRACOTTA)
                .add(Items.PINK_TERRACOTTA)
                .add(Items.GRAY_TERRACOTTA)
                .add(Items.LIGHT_GRAY_TERRACOTTA)
                .add(Items.CYAN_TERRACOTTA)
                .add(Items.PURPLE_TERRACOTTA)
                .add(Items.BLUE_TERRACOTTA)
                .add(Items.BROWN_TERRACOTTA)
                .add(Items.GREEN_TERRACOTTA)
                .add(Items.RED_TERRACOTTA)
                .add(Items.BLACK_TERRACOTTA)
                .add(Items.WHITE_GLAZED_TERRACOTTA)
                .add(Items.ORANGE_GLAZED_TERRACOTTA)
                .add(Items.MAGENTA_GLAZED_TERRACOTTA)
                .add(Items.LIGHT_BLUE_GLAZED_TERRACOTTA)
                .add(Items.YELLOW_GLAZED_TERRACOTTA)
                .add(Items.LIME_GLAZED_TERRACOTTA)
                .add(Items.PINK_GLAZED_TERRACOTTA)
                .add(Items.GRAY_GLAZED_TERRACOTTA)
                .add(Items.LIGHT_GRAY_GLAZED_TERRACOTTA)
                .add(Items.CYAN_GLAZED_TERRACOTTA)
                .add(Items.PURPLE_GLAZED_TERRACOTTA)
                .add(Items.BLUE_GLAZED_TERRACOTTA)
                .add(Items.BROWN_GLAZED_TERRACOTTA)
                .add(Items.GREEN_GLAZED_TERRACOTTA)
                .add(Items.RED_GLAZED_TERRACOTTA)
                .add(Items.BLACK_GLAZED_TERRACOTTA)
        ;

    }
}
