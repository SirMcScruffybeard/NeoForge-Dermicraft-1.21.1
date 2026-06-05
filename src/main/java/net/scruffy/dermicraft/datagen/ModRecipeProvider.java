package net.scruffy.dermicraft.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.masticating.VagueMasticatingRecipe;
import net.scruffy.dermicraft.util.ModMath;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FORCEPS).pattern("I I").pattern(" N ").pattern("I I").define('I', Tags.Items.INGOTS_IRON).define('N', Tags.Items.NUGGETS_IRON).unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON)).save(recipeOutput, getResourceLocation("forceps_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCALPEL.get()).pattern("  I").pattern(" I ").pattern("I  ").define('I', Tags.Items.NUGGETS_IRON).unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON)).save(recipeOutput, getResourceLocation("scalpel_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SUTURE_KIT).pattern("SS ").pattern("SI ").pattern("ISI").define('S', Tags.Items.STRINGS).define('I', Tags.Items.NUGGETS_IRON).unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON)).save(recipeOutput, getResourceLocation("suture_crafting_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SYRINGE).pattern("  N").pattern(" G ").pattern("I  ").define('N', Tags.Items.NUGGETS_IRON).define('G', Tags.Items.GLASS_BLOCKS_CHEAP).define('I', Tags.Items.INGOTS_IRON).unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON)).save(recipeOutput, getResourceLocation("syringe_crafting_table"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModFluids.CRUDE_SLURRY_BUCKET).requires(ModTags.Items.PLANT_FOOD).requires(ModTags.Items.PLANT_FOOD).requires(ModTags.Items.PLANT_FOOD).requires(Tags.Items.BUCKETS_WATER).unlockedBy("has_water_bucket", has(Tags.Items.BUCKETS_WATER)).save(recipeOutput, getResourceLocation("nutrient_slurry_crafting_table"));


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OUTERFACE)
                .pattern("III")
                .pattern("IEI")
                .pattern("INI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('E', ModItems.EYE)
                .define('N', ModItems.NERVE_CLUSTER)
                .unlockedBy("has_inert_tumor", has(ModBlocks.INERT_TUMOR))
                .save(recipeOutput, getResourceLocation("outerface_crafting_table"));

        simpleEarlyImplant(recipeOutput, Tags.Items.FOODS_RAW_MEAT, "inert_tumor_from_implant", ModBlocks.INERT_TUMOR.asItem());

        simpleEarlyImplant(recipeOutput, ModItems.DENSE_MUSCLE.get(), "muscle_tumor_from_implant", ModBlocks.MUSCLE_TUMOR.asItem());
        simpleEarlyImplant(recipeOutput, ModItems.EYE.get(), "eye_tumor_from_implant", ModBlocks.EYE_TUMOR.asItem());
        simpleEarlyImplant(recipeOutput, ModItems.NERVE_CLUSTER.get(), "nerve_tumor_from_implant", ModBlocks.NERVE_TUMOR.asItem());

        buildEarlyImplant(recipeOutput, getResourceLocation("drooling_cauldron_from_implant"),
                List.of(Ingredient.of(net.minecraft.world.level.block.Blocks.CAULDRON),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.NERVE_CLUSTER.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get()),
                        Ingredient.of(ModItems.DENSE_MUSCLE.get())),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS), ModFluids.SOURCE_CRUDE_SLURRY.get(), 100,
                new ItemStack(ModBlocks.DROOLING_CAULDRON.asItem()),
                InventoryChangeTrigger.TriggerInstance.hasItems(Items.CAULDRON));

        buildMasticating(recipeOutput, "calcium_blend_masticating", Ingredient.of(Items.BONE),
                Fluids.WATER, 1000, ModFluids.SOURCE_CALCIUM_BLEND.get(), 1000,
                ModMath.Time.getMinutesToTicks(1),
                InventoryChangeTrigger.TriggerInstance.hasItems(Items.BONE));

        buildVagueMasticating(recipeOutput, "crude_slurry_vague_masticating", Ingredient.of(ModTags.Items.PLANT_FOOD),
                Fluids.WATER, ModFluids.SOURCE_CRUDE_SLURRY.get(),
                InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.MASTICATOR));
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

    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    private ResourceLocation getResourceLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name);
    }

    private Advancement.Builder getAdvancement(RecipeOutput output, ResourceLocation id, Criterion<?> unlockCriterion) {
        return output.advancement().addCriterion("has_the_item", unlockCriterion)
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
    }

    ////////////////////EarlyImplant\\\\\\\\\\\\\\\\\\\\
    private void buildEarlyImplant(RecipeOutput output, ResourceLocation id, List<Ingredient> ingredients, Ingredient sutureTool, Fluid fluid, int amount, ItemStack result, Criterion<?> unlockCriterion) {

        FluidStack fluidStack = new FluidStack(fluid, amount);

        EarlyImplantRecipe recipe = new EarlyImplantRecipe(ingredients, sutureTool, fluidStack, result);

        Advancement.Builder advancementBuilder = output.advancement().addCriterion("has_the_item", unlockCriterion).rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);

        output.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/")));
    }

    private void simpleEarlyImplant(RecipeOutput recipeOutput, Item ingredient, String name, Item result) {
        buildEarlyImplant(recipeOutput, getResourceLocation(name), List.of(Ingredient.of(ingredient)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 100,
                new ItemStack(result),
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(ItemTags.DIRT).build()));
    }

    private void simpleEarlyImplant(RecipeOutput recipeOutput, TagKey<Item> ingredient, String name, Item result) {
        buildEarlyImplant(recipeOutput, getResourceLocation(name), List.of(Ingredient.of(ingredient)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 100,
                new ItemStack(result),
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(ItemTags.DIRT).build()));
    }

    ////////////////////Masticating\\\\\\\\\\\\\\\\\\\\
    private void buildMasticating(RecipeOutput output, String name, Ingredient ingredient, Fluid fluid, int ingredientFluidAmount,
                                  Fluid result, int resultAmount, int ticks, Criterion<?> unlockCriterion) {
        ResourceLocation id = getResourceLocation(name);

        MasticatingRecipe recipe = new MasticatingRecipe(ingredient, fluid, ingredientFluidAmount, result, resultAmount, ticks);

        Advancement.Builder advancementBuilder = output.advancement().addCriterion("has_the_item", unlockCriterion)
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);

        output.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/")));
    }

    private void buildVagueMasticating(RecipeOutput output, String name, Ingredient ingredient,
                                       Fluid fluid, Fluid result, Criterion<?> unlockCriterion) {

        ResourceLocation id = getResourceLocation(name);

        VagueMasticatingRecipe recipe = new VagueMasticatingRecipe(ingredient, fluid, result);

        output.accept(id, recipe, getAdvancement(output, id, unlockCriterion).build(id.withPrefix("recipes/")));
    }

}
