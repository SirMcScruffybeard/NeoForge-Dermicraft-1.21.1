package net.scruffy.dermicraft.datagen.recipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
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
import net.scruffy.dermicraft.recipe.dipping.DippingRecipe;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.recipe.effluencing.EffluencingRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeBuilders {


    ////////////////////Drooling\\\\\\\\\\\\\\\\\\\\
    public static void buildVagueDrooling(RecipeOutput output, String name, Ingredient ingredient, float modifier,
                                          Fluid result) {

        ResourceLocation id = getResourceLocation(name);
        VagueDroolingRecipe recipe = new VagueDroolingRecipe(ingredient, modifier, result);
        output.accept(id, recipe, null);
    }

    ////////////////////EarlyImplant\\\\\\\\\\\\\\\\\\\\
    public static void buildEarlyImplant(RecipeOutput output, String name, List<Ingredient> ingredients,
                                         Ingredient sutureTool, Fluid fluid, int amount, Item result) {
        ResourceLocation id = getResourceLocation(name);
        FluidStack fluidStack = new FluidStack(fluid, amount);
        EarlyImplantRecipe recipe = new EarlyImplantRecipe(ingredients, sutureTool, fluidStack, result);
        output.accept(id, recipe, null);
    }

    public static void simpleEarlyImplant(RecipeOutput recipeOutput, Item ingredient, String name, Item result) {
        buildEarlyImplant(recipeOutput, name, List.of(Ingredient.of(ingredient)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 100,
                result);
    }

    public static void simpleEarlyImplant(RecipeOutput recipeOutput, TagKey<Item> ingredient, String name, Item result) {
        buildEarlyImplant(recipeOutput, name, List.of(Ingredient.of(ingredient)),
                Ingredient.of(ModTags.Items.SUTURE_TOOLS),
                ModFluids.SOURCE_CRUDE_SLURRY.get(), 100,
                result);
    }

    ////////////////////Masticating\\\\\\\\\\\\\\\\\\\\
    public static void buildMasticating(RecipeOutput output, String name, Ingredient ingredient, int itemAmount, Fluid fluid,
                                        int ingredientFluidAmount, Fluid result, int resultAmount, float modifier, int ticks) {
        ResourceLocation id = getResourceLocation(name);
        MasticatingRecipe recipe = new MasticatingRecipe(ingredient, itemAmount, fluid, ingredientFluidAmount,
                result, resultAmount, modifier, ticks);
        output.accept(id, recipe, null);
    }

    public static void masticateWithWater(RecipeOutput recipeOutput, String name, Item item, int ingredientFluidAmount,
                                          Fluid result, int resultAmount, int ticks) {
        buildMasticating(recipeOutput, name, Ingredient.of(item), 1,
                Fluids.WATER, ingredientFluidAmount, result, resultAmount, -1, ticks);
    }

    public static void masticateWithWater(RecipeOutput recipeOutput, String name, TagKey<Item> itemTag, int ingredientFluidAmount,
                                          Fluid result, int resultAmount, int ticks) {
        buildMasticating(recipeOutput, name, Ingredient.of(itemTag), 1,
                Fluids.WATER, ingredientFluidAmount, result, resultAmount, -1, ticks);
    }

    public static void vagueMasticateWithTagAndWater(RecipeOutput recipeOutput, String name, TagKey<Item> itemTag, float modifier,
                                                     Fluid result) {
        buildMasticating(recipeOutput, name, Ingredient.of(itemTag), 1,
                Fluids.WATER, -1, result, -1, modifier, -50);
    }


    ////////////////////Effluencing\\\\\\\\\\\\\\\\\\\\
    public static void buildEffluencing(RecipeOutput output, String name, Fluid fluidA, int fluidAAmount,
                                        Fluid fluidB, int fluidBAmount, Fluid result, int resultAmount, int ticks) {
        ResourceLocation id = getResourceLocation(name);
        EffluencingRecipe recipe = new EffluencingRecipe(fluidA, fluidAAmount, fluidB, fluidBAmount,
                result, resultAmount, ticks);
        output.accept(id, recipe, null);
    }


    ////////////////////Dipping\\\\\\\\\\\\\\\\\\\\
    public static void buildDipping(RecipeOutput output, String name, Ingredient ingredient, int itemAmount, Fluid fluid,
                                    int ingredientFluidAmount, Item result, int resultAmount) {
        ResourceLocation id = getResourceLocation(name);
        DippingRecipe recipe = new DippingRecipe(ingredient, itemAmount, fluid, ingredientFluidAmount, result, resultAmount);
        output.accept(id, recipe, null);
    }

    public static void simpleDipping(RecipeOutput output, String name, Item ingredient, int itemAmount, Fluid fluid,
                                     int ingredientFluidAmount, Item result, int resultAmount) {
        buildDipping(output, name, Ingredient.of(ingredient), itemAmount, fluid, ingredientFluidAmount, result, resultAmount);
    }

    public static void simpleDipping(RecipeOutput output, String name, TagKey<Item> ingredientTag, int itemAmount, Fluid fluid,
                                     int ingredientFluidAmount, Item result, int resultAmount) {
        buildDipping(output, name, Ingredient.of(ingredientTag), itemAmount, fluid, ingredientFluidAmount, result, resultAmount);
    }


    ////////////////////Puddle Craft\\\\\\\\\\\\\\\\\\\\
    public static class PuddleCraft {

        public static void build(RecipeOutput output, String name, List<Ingredient> ingredients,
                                 Fluid puddle, Fluid resultFluid, Item resultItem, int resultItemAmount, int ticks) {
            ResourceLocation id = getResourceLocation(name);
            PuddleCraftingRecipe recipe = new PuddleCraftingRecipe(ingredients, puddle,
                    resultFluid, resultItem, resultItemAmount, ticks);

            output.accept(id, recipe, null);
        }

        public static void buildFromTag(RecipeOutput output, String name, TagKey<Item> ingredientTag, int amount,
                                        Fluid puddle, Fluid resultFluid, Item resultItem, int resultItemAmount, int ticks) {

            List<Ingredient> ingredients = new ArrayList<>();

            for (int i = 0; i < amount; i++) {
                ingredients.add(Ingredient.of(ingredientTag));
            }

            build(output, name, ingredients, puddle, resultFluid, resultItem, resultItemAmount, ticks);
        }

        public static class MakeFluids {

            public static void make(RecipeOutput output, String name, List<Ingredient> ingredients,
                             Fluid puddle, Fluid resultFluid, int ticks) {

                build(output, name, ingredients, puddle, resultFluid, Items.AIR, 0, ticks);
            }

            public static void makeFromTag(RecipeOutput output, String name, TagKey<Item> ingredientTag, int amount, Fluid puddle,
                                    Fluid resultFluid, int ticks) {
                List<Ingredient> ingredients = new ArrayList<>();
                for (int i = 0; i < amount; i++) {
                    ingredients.add(Ingredient.of(ingredientTag));
                }
                make(output, name, ingredients, puddle, resultFluid, ticks);
            }
        }

        public static class MakeItems {

            public static void make(RecipeOutput output, String name, List<Ingredient> ingredients,
                             Fluid puddle, Item resultItem, int resultItemAmount,
                             int ticks) {
                build(output, name, ingredients, puddle, Fluids.EMPTY, resultItem, resultItemAmount, ticks);
            }

            public static void makeFromTag(RecipeOutput output, String name, TagKey<Item> ingredientTag, int amount, Fluid puddle,
                                    Item resultItem, int resultAmount, int ticks) {
                List<Ingredient> ingredients = new ArrayList<>();
                for (int i = 0; i < amount; i++) {
                    ingredients.add(Ingredient.of(ingredientTag));
                }
                make(output, name, ingredients, puddle, resultItem, resultAmount, ticks);
            }

            public static void makeFromOneItem(RecipeOutput output, String name, Item ingredient, int amount, Fluid puddle,
                                        Item resultItem, int resultAmount, int ticks) {
                List<Ingredient> ingredients = new ArrayList<>();
                for (int i = 0; i < amount; i++) {
                    ingredients.add(Ingredient.of(ingredient));
                }
                make(output, name, ingredients, puddle, resultItem, resultAmount, ticks);
            }
        }
    }


    ////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\
    public static ResourceLocation getResourceLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, name);
    }
}
