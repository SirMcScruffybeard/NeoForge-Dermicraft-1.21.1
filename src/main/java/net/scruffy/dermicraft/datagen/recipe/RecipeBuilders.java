package net.scruffy.dermicraft.datagen.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.masticating.VagueMasticatingRecipe;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeBuilders {


    ////////////////////Drooling\\\\\\\\\\\\\\\\\\\\
    public void buildVagueDrooling(RecipeOutput output, String name, Ingredient ingredient, float modifier,
                                   Fluid result, Criterion<?> unlockCriterion) {

        ResourceLocation id = getResourceLocation(name);
        VagueDroolingRecipe recipe = new VagueDroolingRecipe(ingredient, modifier, result);
        output.accept(id, recipe, getAdvancement(output, id, unlockCriterion).build(id.withPrefix("recipes/")));
    }

    ////////////////////EarlyImplant\\\\\\\\\\\\\\\\\\\\
    public void buildEarlyImplant(RecipeOutput output, ResourceLocation id, List<Ingredient> ingredients,
                                  Ingredient sutureTool, Fluid fluid, int amount, Item result, Criterion<?> unlockCriterion) {

        FluidStack fluidStack = new FluidStack(fluid, amount);
        EarlyImplantRecipe recipe = new EarlyImplantRecipe(ingredients, sutureTool, fluidStack, result);
        Advancement.Builder advancementBuilder = output.advancement().addCriterion("has_the_item", unlockCriterion)
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
        output.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/")));
    }

    public void simpleEarlyImplant(RecipeOutput recipeOutput, Item ingredient, String name, Item result) {
        buildEarlyImplant(recipeOutput, getResourceLocation(name), List.of(Ingredient.of(ingredient)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 100,
                result,
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(ItemTags.DIRT).build()));
    }

    public void simpleEarlyImplant(RecipeOutput recipeOutput, TagKey<Item> ingredient, String name, Item result) {
        buildEarlyImplant(recipeOutput, getResourceLocation(name), List.of(Ingredient.of(ingredient)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 100,
                result,
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(ItemTags.DIRT).build()));
    }

    ////////////////////Masticating\\\\\\\\\\\\\\\\\\\\
    public void buildMasticating(RecipeOutput output, String name, Ingredient ingredient, Fluid fluid, int ingredientFluidAmount,
                                 Fluid result, int resultAmount, int ticks, Criterion<?> unlockCriterion) {
        ResourceLocation id = getResourceLocation(name);
        MasticatingRecipe recipe = new MasticatingRecipe(ingredient, fluid, ingredientFluidAmount, result, resultAmount, ticks);
        Advancement.Builder advancementBuilder = output.advancement().addCriterion("has_the_item", unlockCriterion)
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
        output.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/")));
    }

    public void buildVagueMasticating(RecipeOutput output, String name, Ingredient ingredient, float modifier,
                                      Fluid fluid, Fluid result, Criterion<?> unlockCriterion) {
        ResourceLocation id = getResourceLocation(name);
        VagueMasticatingRecipe recipe = new VagueMasticatingRecipe(ingredient, modifier, fluid, result);
        output.accept(id, recipe, getAdvancement(output, id, unlockCriterion).build(id.withPrefix("recipes/")));
    }

    public void masticateWithWater(RecipeOutput recipeOutput, String name, Item item, int ingredientFluidAmount,
                                   Fluid result, int resultAmount, int ticks) {
        buildMasticating(recipeOutput, name, Ingredient.of(item),
                Fluids.WATER, ingredientFluidAmount, result, resultAmount, ticks,
                InventoryChangeTrigger.TriggerInstance.hasItems(item));
    }

    public void vagueMasticateWithTagAndWater(RecipeOutput recipeOutput, String name, TagKey<Item> itemTag, float modifier,
                                              Fluid result, Item advItem) {
        buildVagueMasticating(recipeOutput, name, Ingredient.of(itemTag), modifier, Fluids.WATER, result,
                InventoryChangeTrigger.TriggerInstance.hasItems(advItem));
    }


    ////////////////////Puddle Craft\\\\\\\\\\\\\\\\\\\\
    public static class PuddleCraft {

        public static void build(RecipeOutput output, String name, List<Ingredient> ingredients,
                                 Fluid puddle, Fluid resultFluid, Item resultItem, int resultItemAmount, int ticks,
                                 Criterion<?> unlockCriterion) {
            ResourceLocation id = getResourceLocation(name);
            PuddleCraftingRecipe recipe = new PuddleCraftingRecipe(ingredients, puddle,
                    resultFluid, resultItem, resultItemAmount, ticks);

            output.accept(id, recipe, getAdvancement(output, id, unlockCriterion).build(id.withPrefix("recipes/")));
        }

        public static void buildFromTag(RecipeOutput output, String name, TagKey<Item> ingredientTag, int amount,
                                        Fluid puddle, Fluid resultFluid, Item resultItem, int resultItemAmount, int ticks,
                                        Item advanceItem) {

            List<Ingredient> ingredients = new ArrayList<>();

            for (int i = 0; i < amount; i++) {
                ingredients.add(Ingredient.of(ingredientTag));
            }

            build(output, name, ingredients, puddle, resultFluid, resultItem, resultItemAmount, ticks,
                    InventoryChangeTrigger.TriggerInstance.hasItems(advanceItem));
        }

        public static class MakeFluids {

            public void make(RecipeOutput output, String name, List<Ingredient> ingredients,
                             Fluid puddle, Fluid resultFluid, int ticks, Item advanceItem) {

                build(output, name, ingredients, puddle, resultFluid, Items.AIR, 0, ticks,
                        InventoryChangeTrigger.TriggerInstance.hasItems(advanceItem));
            }

            public void makeFromTag(RecipeOutput output, String name, TagKey<Item> ingredientTag, int amount, Fluid puddle,
                                    Fluid resultFluid, int ticks, Item advanceItem) {
                List<Ingredient> ingredients = new ArrayList<>();
                for (int i = 0; i < amount; i++) {
                    ingredients.add(Ingredient.of(ingredientTag));
                }
                make(output, name, ingredients, puddle, resultFluid, ticks, advanceItem);
            }
        }

        public static class MakeItems {

            public void make(RecipeOutput output, String name, List<Ingredient> ingredients,
                             Fluid puddle, Item resultItem, int resultItemAmount,
                             int ticks, Item advanceItem) {
                build(output, name, ingredients, puddle, Fluids.EMPTY, resultItem, resultItemAmount, ticks,
                        InventoryChangeTrigger.TriggerInstance.hasItems(advanceItem));
            }

            public void makeFromTag(RecipeOutput output, String name, TagKey<Item> ingredientTag, int amount, Fluid puddle,
                                    Item resultItem, int resultAmount, int ticks, Item advanceItem) {
                List<Ingredient> ingredients = new ArrayList<>();
                for (int i = 0; i < amount; i++) {
                    ingredients.add(Ingredient.of(ingredientTag));
                }

                make(output, name, ingredients, puddle, resultItem, resultAmount, ticks, advanceItem);
            }
        }
    }


    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    public static ResourceLocation getResourceLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name);
    }

    public static Advancement.Builder getAdvancement(RecipeOutput output, ResourceLocation id, Criterion<?> unlockCriterion) {
        return output.advancement().addCriterion("has_the_item", unlockCriterion)
                .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
    }
}
