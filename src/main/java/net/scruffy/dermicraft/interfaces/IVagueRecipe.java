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

    /**
     * The old pre-1.20.5 "saturation modifier" coefficient (roughly 0-1.2), NOT
     * {@code FoodProperties#saturation()} directly -- Mojang redefined that field to be the
     * precomputed actual saturation restored ({@code nutrition * oldModifier * 2}), not the small
     * multiplier this class's whole formula (and every recipe/modifier value calibrated against it)
     * was designed around. Recovering the old coefficient from the new field is the inverse of that
     * formula. Using {@code props.saturation()} raw here inflates {@link #getFoodWeight} massively
     * for high-nutrition/high-saturation foods -- confirmed via cooked porkchop (nutrition 8,
     * modifier 0.8): the new field reports 12.8 (=8*0.8*2), which without this fix pushed
     * protein_blend_vague_masticating's water requirement to 7176 mB, past the Masticator's 5000 mB
     * tank cap, silently making it impossible to ever craft rather than throwing an error.
     */
    default float getSaturation(ItemStack stack) {
        FoodProperties props = stack.getFoodProperties(null);
        if (props == null) return 0;
        float nutrition = props.nutrition();
        if (nutrition <= 0) return 0;
        return props.saturation() / (nutrition * 2);
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
