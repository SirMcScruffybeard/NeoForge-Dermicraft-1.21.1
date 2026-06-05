package net.scruffy.dermicraft.recipe.masticating;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.ModRecipes;

public record VagueMasticatingRecipe(Ingredient ingredientItem, Fluid ingredientFluid,
                                     Fluid result) implements Recipe<OneFluidOneItemRecipeInput> {


    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredientItem);
        return list;
    }

    @Override
    public boolean matches(OneFluidOneItemRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        return testIngredient(input.getItem(0))
                && testFluid(input.getFluid());
    }

    public boolean matches(Level level, ItemStack stack, FluidStack fluidStack) {
        return matches(new OneFluidOneItemRecipeInput(stack, fluidStack), level);
    }

    public boolean testIngredient(ItemStack stack) {
        return ingredientItem.test(stack);
    }

    public boolean testFluid(FluidStack fluidStack) {
        return ingredientFluid.isSame(fluidStack.getFluid());
    }

    @Override
    public ItemStack assemble(OneFluidOneItemRecipeInput vagueMasticatingRecipeInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.VAGUE_MASTICATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.VAGUE_MASTICATING_TYPE.get();
    }

    /////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\
    public boolean isPlantFood(ItemStack stack) {
        return stack.is(ModTags.Items.PLANT_FOOD);
    }

    public boolean isMeatFood(ItemStack stack) {
        return stack.is(ModTags.Items.MEAT_FOOD);
    }

    public boolean isPartItem(ItemStack stack) {
        return stack.is(ModTags.Items.PART_ITEMS);
    }

    public boolean hasNutrition(ItemStack stack) {
        FoodProperties prop = stack.getFoodProperties(null);
        return prop != null && prop.nutrition() > 0;
    }

    public float getNutrition(ItemStack stack) {
        if (!hasNutrition(stack)) return 0;
        return stack.getFoodProperties(null).nutrition();
    }

    public float getSaturation(ItemStack stack) {
        FoodProperties props = stack.getFoodProperties(null);
        if (props == null) return 0;
        return props.saturation();
    }

    public float getFoodWeight(ItemStack stack) {
        return getNutrition(stack) * (getSaturation(stack) + 1);
    }

    public int getCraftingTime(ItemStack stack) {
        int baseTicks = 20;
        return Math.round(baseTicks * (getFoodWeight(stack)));
    }

    public int getCraftingAmount(ItemStack stack) {
        int baseMultiplier =  25;
        return Math.round(baseMultiplier * getFoodWeight(stack));
    }

    public FluidStack getResultStack(int amount) {
        return new FluidStack(result, amount);
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<VagueMasticatingRecipe> {

        @Override
        public MapCodec<VagueMasticatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VagueMasticatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<VagueMasticatingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(VagueMasticatingRecipe::ingredientItem),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("ingredient_fluid").forGetter(VagueMasticatingRecipe::ingredientFluid),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(VagueMasticatingRecipe::result))
                                .apply(inst, VagueMasticatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, VagueMasticatingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, VagueMasticatingRecipe::ingredientItem,
                        ByteBufCodecs.registry(Registries.FLUID), VagueMasticatingRecipe::ingredientFluid,
                        ByteBufCodecs.registry(Registries.FLUID), VagueMasticatingRecipe::result,
                        VagueMasticatingRecipe::new);
    }
}
