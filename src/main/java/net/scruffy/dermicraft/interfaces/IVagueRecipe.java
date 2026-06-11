package net.scruffy.dermicraft.interfaces;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public interface IVagueRecipe {

    default boolean hasNutrition(ItemStack stack) {
        FoodProperties prop = stack.getFoodProperties(null);
        return prop != null && prop.nutrition() > 0;
    }

    default float getNutrition(ItemStack stack) {
        if (!hasNutrition(stack)) return 0;
        return stack.getFoodProperties(null).nutrition();
    }

    default float getSaturation(ItemStack stack) {
        FoodProperties props = stack.getFoodProperties(null);
        if (props == null) return 0;
        return props.saturation();
    }

    default float getFoodWeight(ItemStack stack) {
        return getNutrition(stack) * (getSaturation(stack) + 1);
    }

    default int getCraftingTime(ItemStack stack, int baseTicks) {
        return Math.round(baseTicks * (getFoodWeight(stack)));
    }
    int getCraftingTime(ItemStack stack);

    int getCraftingAmount(ItemStack stack);

    default int getCraftingAmount(ItemStack stack, float modifier) {
        int baseMultiplier =  25;
        return Math.round(baseMultiplier * getFoodWeight(stack) * modifier);
    }


    FluidStack getResultFluidStack(int amount);


}
