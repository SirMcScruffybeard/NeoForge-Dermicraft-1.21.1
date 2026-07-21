package net.scruffy.dermicraft.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One concrete JEI display entry for a nutrition-scaled ("vague") recipe -- pairs the source
 * recipe with a single matching ingredient stack and the yield/time computed specifically for
 * that stack, since {@link net.scruffy.dermicraft.interfaces.IVagueRecipe} yields can differ
 * per item even when several items share one {@code Ingredient} match (e.g. a tag). JEI shows
 * one entry per concrete item rather than trying to animate/cycle a single dynamic result.
 */
public record DynamicRecipeDisplay<T extends Recipe<?>>(
        RecipeHolder<T> source,
        ItemStack concreteIngredient,
        FluidStack computedResult,
        int computedTicks
) {
}
