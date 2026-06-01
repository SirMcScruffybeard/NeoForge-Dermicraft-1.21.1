package net.scruffy.dermicraft.recipe.masticating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.recipe.ModRecipes;


public record MasticatingRecipe(Ingredient ingredient, Fluid ingredientFluid, int ingredientFluidAmount ,
                                Fluid result, int resultAmount, int ticks) implements Recipe<MasticatingRecipeInput> {

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override
    public boolean matches(MasticatingRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        return testIngredient(input.getItem(0))
                && testFluid(input.getFluid());
    }

    public boolean matches(Level level, ItemStack stack, FluidStack fluidStack) {
        return matches(new MasticatingRecipeInput(stack, fluidStack), level);
    }

    public boolean testIngredient(ItemStack stack) {
        return ingredient.test(stack);
    }

    public boolean testFluid(FluidStack fluidStack) {
        return ingredientFluid.isSame(fluidStack.getFluid())
                && fluidStack.getAmount() >= ingredientFluidAmount;
    }

    @Override
    public ItemStack assemble(MasticatingRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MASTICATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MASTICATING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<MasticatingRecipe> {

        @Override
        public MapCodec<MasticatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MasticatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<MasticatingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(MasticatingRecipe::ingredient),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("ingredient_fluid").forGetter(MasticatingRecipe::ingredientFluid),
                                Codec.INT.fieldOf("ingredient_fluid_amount").forGetter(MasticatingRecipe::ingredientFluidAmount),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(MasticatingRecipe::result),
                                Codec.INT.fieldOf("result_fluid_amount").forGetter(MasticatingRecipe::resultAmount),
                                Codec.INT.fieldOf("ticks").forGetter(MasticatingRecipe::ticks)).apply(inst, MasticatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MasticatingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, MasticatingRecipe::ingredient,
                        ByteBufCodecs.registry(Registries.FLUID), MasticatingRecipe::ingredientFluid,
                        ByteBufCodecs.VAR_INT, MasticatingRecipe::ingredientFluidAmount,
                        ByteBufCodecs.registry(Registries.FLUID), MasticatingRecipe::result,
                        ByteBufCodecs.VAR_INT, MasticatingRecipe::resultAmount,
                        ByteBufCodecs.VAR_INT, MasticatingRecipe::ticks,
                        MasticatingRecipe::new);
    }
}
