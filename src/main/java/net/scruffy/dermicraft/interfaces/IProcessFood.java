package net.scruffy.dermicraft.interfaces;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;

public interface IProcessFood {

    default boolean isPlantFood(ItemStack item) {
        return item.is(ModTags.Items.PLANT_FOOD);
    }

    default boolean isMeatFood(ItemStack item) {
        return item.is(ModTags.Items.MEAT_FOOD);
    }

    default boolean isFood(ItemStack stack) {
      return (isPlantFood(stack) || isMeatFood(stack) || isPartItem(stack)) && hasNutrition(stack);
    }

    default boolean isPartItem(ItemStack stack) {
        return stack.is(ModTags.Items.PART_ITEMS);
    }

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

    default float getSaturationModifier(ItemStack stack, int ticks) {
        return getSaturation(stack) * ticks;
    }

    default int getProcessTime(ItemStack stack, int ticks) {
        return(int)(40 + getNutrition(stack) * ticks);
    }

    /****************************************************************************************
     *
     * @param stack
     * @param modifier total of extra modifiers to be used. If there aren't any use 1
     * @param min minimum amount to be made
     * @return the amount of ingredientFluid to be made in mB
     ***************************************************************************************/
    default int getProcessAmount(ItemStack stack, float modifier, int min) {
        if(!hasNutrition(stack)) return 0;
        return (int)Math.max(min, (getNutrition(stack)* modifier));
    }

    default FluidStack createWater(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    default  FluidStack createNutrientSlurry(int amount) {
        return new FluidStack(ModFluids.SOURCE_NUTRIENT_SLURRY.get(), amount);
    }




    default void consumeItem(IItemHandler inventory, int slot, int amount) {
        inventory.extractItem(slot, amount, false);
    }
    default void consumeSingleItem(IItemHandler inventory, int slot) {
        consumeItem(inventory, slot, 1);
    }

}
