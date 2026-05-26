package net.scruffy.dermicraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FORCEPS)
                .pattern("I I")
                .pattern(" N ")
                .pattern("I I")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(recipeOutput, getResourceLocation("forceps_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCALPEL.get())
                .pattern("  I")
                .pattern(" I ")
                .pattern("I  ")
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, getResourceLocation("scalpel_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SUTURE_KIT)
                .pattern("SS ")
                .pattern("SI ")
                .pattern("ISI")
                .define('S', Tags.Items.STRINGS)
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, getResourceLocation("suture_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SYRINGE)
                .pattern("  N")
                .pattern(" G ")
                .pattern("I  ")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, getResourceLocation("syringe_crafting_table"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModFluids.NUTRIENT_SLURRY_BUCKET)
                .requires(ModTags.Items.PLANT_FOOD)
                .requires(ModTags.Items.PLANT_FOOD)
                .requires(ModTags.Items.PLANT_FOOD)
                .requires(Tags.Items.BUCKETS_WATER)
                .unlockedBy("has_water_bucket", has(Tags.Items.BUCKETS_WATER))
                .save(recipeOutput, getResourceLocation("nutrient_slurry_crafting_table"));


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OUTERFACE)
                .pattern("III")
                .pattern("IEI")
                .pattern("INI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('E', ModItems.EYE)
                .define('N', ModItems.NERVE_CLUSTER)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, getResourceLocation("outerface_crafting_table"));
    }

    ////////////////////Other Crafting Methods\\\\\\\\\\\\\\\\\\\\
    protected static void oreSmelting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput pRecipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pRecipeOutput, Dermicraft.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }

    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    private ResourceLocation getResourceLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name);
    }
}
