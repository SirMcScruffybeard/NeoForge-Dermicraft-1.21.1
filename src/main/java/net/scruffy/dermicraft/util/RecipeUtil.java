package net.scruffy.dermicraft.util;

import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeUtil {

    public static boolean isRecipeValid(RecipeHolder<?> recipe) {
        return recipe != null && recipe.value() != null;
    }
}
