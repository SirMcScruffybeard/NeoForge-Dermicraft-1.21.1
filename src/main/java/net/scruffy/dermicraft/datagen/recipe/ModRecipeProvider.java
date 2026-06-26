package net.scruffy.dermicraft.datagen.recipe;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FORCEPS)
                .pattern("I I")
                .pattern(" N ")
                .pattern("I I")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("forceps_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCALPEL.get())
                .pattern("  I")
                .pattern(" I ")
                .pattern("I  ")
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("scalpel_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SUTURE_KIT)
                .pattern("SS ").pattern("SI ")
                .pattern("ISI")
                .define('S', Tags.Items.STRINGS)
                .define('I', Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON)).
                save(recipeOutput, RecipeBuilders.getResourceLocation("suture_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SYRINGE)
                .pattern("  N")
                .pattern(" G ")
                .pattern("I  ")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("syringe_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.GLASS_FLASK, 4)
                .pattern(" G ")
                .pattern(" G ")
                .pattern("G G")
                .define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .unlockedBy("has_glass", has(Tags.Items.GLASS_BLOCKS_CHEAP))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("flask_crafting_table"));


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OUTERFACE)
                .pattern("III")
                .pattern("IEI")
                .pattern("INI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('E', ModItems.EYE)
                .define('N', ModItems.NERVE_CLUSTER)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, RecipeBuilders.getResourceLocation("outerface_crafting_table"));

        RecipeBuilders.simpleEarlyImplant(recipeOutput, Tags.Items.FOODS_RAW_MEAT, "inert_tumor_implant", ModBlocks.INERT_TUMOR.asItem(), ModBlocks.INERT_TUMOR.asItem());

        RecipeBuilders.simpleEarlyImplant(recipeOutput, ModItems.DENSE_MUSCLE.get(), "muscle_tumor_from_implant", ModBlocks.MUSCLE_TUMOR.asItem(), ModItems.DENSE_MUSCLE.get());
        RecipeBuilders.simpleEarlyImplant(recipeOutput, ModItems.EYE.get(), "eye_tumor_implant", ModBlocks.EYE_TUMOR.asItem(), ModItems.EYE.get());
        RecipeBuilders.simpleEarlyImplant(recipeOutput, ModItems.NERVE_CLUSTER.get(), "nerve_tumor_implant", ModBlocks.NERVE_TUMOR.asItem(), ModItems.NERVE_CLUSTER.get());

        RecipeBuilders.buildEarlyImplant(recipeOutput,"drooling_cauldron_implant",
                List.of(Ingredient.of(Blocks.CAULDRON),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.DROOLING_CAULDRON.asItem(),
                Items.CAULDRON);

        RecipeBuilders.buildEarlyImplant(recipeOutput, "masticator_implant",
                List.of(
                        Ingredient.of(Items.BONE),
                        Ingredient.of(Items.BONE),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(), 100,
                ModBlocks.MASTICATOR.asItem(),
                ModBlocks.INERT_TUMOR.asItem());

        //TODO skin tank recipe

        RecipeBuilders.buildVagueDrooling(recipeOutput, "water_drooling", Ingredient.of(Tags.Items.FOODS), 1, Fluids.WATER,
                InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.DROOLING_CAULDRON));

        RecipeBuilders.buildMasticating(recipeOutput, "calcium_blend_bone_masticating", Ingredient.of(Items.BONE),
                Fluids.WATER, 1000, ModFluids.SOURCE_CALCIUM_BLEND.get(), 1000,
                ModMath.Time.getMinutesToTicks(1),
                InventoryChangeTrigger.TriggerInstance.hasItems(Items.BONE));

        RecipeBuilders.buildMasticating(recipeOutput, "calcium_blend_bone_meal_masticating", Ingredient.of(Items.BONE_MEAL),
                Fluids.WATER, 334, ModFluids.SOURCE_CALCIUM_BLEND.get(), 330,
                ModMath.Time.getMinutesToTicks(1),
                InventoryChangeTrigger.TriggerInstance.hasItems(Items.BONE_MEAL));


        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_coal_block", Items.COAL_BLOCK, 1000,
                ModFluids.SOURCE_CARBON_BLEND.get(), 1000, ModMath.Time.getSecondsToTicks(60));

        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_coal", Items.COAL, 110,
                ModFluids.SOURCE_CARBON_BLEND.get(), 112, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.masticateWithWater(recipeOutput, "carbon_blend_masticating_charcoal", Items.CHARCOAL, 110,
                ModFluids.SOURCE_CARBON_BLEND.get(), 11, ModMath.Time.getSecondsToTicks(30));

        RecipeBuilders.vagueMasticateWithTagAndWater(recipeOutput, "crude_slurry_vague_masticating", ModTags.Items.PLANT_FOOD, 1,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), ModBlocks.DROOLING_CAULDRON.asItem());

        RecipeBuilders.vagueMasticateWithTagAndWater(recipeOutput, "protein_blend_vague_masticating", ModTags.Items.MEAT_FOOD, 1,
                ModFluids.SOURCE_PROTEIN_BLEND.get(), ModBlocks.DROOLING_CAULDRON.asItem());


        RecipeBuilders.PuddleCraft.MakeFluids.makeFromTag(recipeOutput, "crude_slurry_puddle", ModTags.Items.PLANT_FOOD, 4, Fluids.WATER,
                ModFluids.SOURCE_CRUDE_SLURRY.get(), ModMath.Time.getSecondsToTicks(10), Items.WATER_BUCKET);

        RecipeBuilders.PuddleCraft.MakeFluids.make(recipeOutput, "primitive_catalyst_puddle_coal",
                List.of(Ingredient.of(Items.COAL), Ingredient.of(Items.BONE), Ingredient.of(Tags.Items.FOODS_RAW_MEAT)),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(),
                ModMath.Time.getSecondsToTicks(10), ModFluids.CRUDE_SLURRY_BUCKET.get());

        RecipeBuilders.PuddleCraft.MakeFluids.make(recipeOutput, "primitive_catalyst_puddle_charcoal",
                List.of(Ingredient.of(Items.CHARCOAL), Ingredient.of(Items.BONE), Ingredient.of(Tags.Items.FOODS_RAW_MEAT)),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), ModFluids.SOURCE_PRIMITIVE_CATALYST.get(),
                ModMath.Time.getSecondsToTicks(10), ModFluids.CRUDE_SLURRY_BUCKET.get());

        RecipeBuilders.PuddleCraft.MakeItems.makeFromTag(recipeOutput, "inert_tumor_puddle", ModTags.Items.ANIMAL_MEATS, 4, ModFluids.SOURCE_CRUDE_SLURRY.get(),
                ModBlocks.INERT_TUMOR.asItem(), 1, ModMath.Time.getSecondsToTicks(10), ModFluids.CRUDE_SLURRY_BUCKET.get());

        RecipeBuilders.PuddleCraft.MakeItems.makeFromOneItem(recipeOutput, "calcium_glass_puddle", Items.BONE_MEAL, 1, ModFluids.SOURCE_CALCIUM_BLEND.get(),
                ModBlocks.CALCIUM_GLASS.asItem(), 1, ModMath.Time.getSecondsToTicks(20),  ModBlocks.CALCIUM_GLASS.asItem());
    }

    ////////////////////Other Crafting Methods\\\\\\\\\\\\\\\\\\\\
    protected static void oreSmelting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult, pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput pRecipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for (ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike)).save(pRecipeOutput, Dermicraft.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
