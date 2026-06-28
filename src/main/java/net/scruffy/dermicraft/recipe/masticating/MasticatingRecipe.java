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
import net.scruffy.dermicraft.interfaces.IVagueRecipe;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import org.jetbrains.annotations.NotNull;


public record MasticatingRecipe(Ingredient ingredient, int itemAmount, Fluid ingredientFluid, int ingredientFluidAmount,
                                Fluid result, int resultAmount, float modifier, int ticks)
        implements Recipe<OneFluidOneItemRecipeInput>, IVagueRecipe {

    @Override @NotNull
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
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
        if (isDynamicAmount() || isDynamicTime()) {
            if (!hasNutrition(stack)) return false;
        }
        return ingredient.test(stack) && stack.getCount() >= itemAmount;
    }

    public boolean testFluid(FluidStack fluidStack) {
        if (!ingredientFluid.isSame(fluidStack.getFluid())) return false;
        return ingredientFluidAmount < 0 || fluidStack.getAmount() >= ingredientFluidAmount;
    }

    private boolean isDynamicAmount() {
        return resultAmount < 0;
    }

    private boolean isDynamicTime() {
        return ticks < 0;
    }

    @Override
    public int getCraftingAmount(ItemStack stack) {
        if (isDynamicAmount()) return getCraftingAmount(stack, modifier);
        return resultAmount;
    }

    @Override
    public int getCraftingTime(ItemStack stack) {
        if (isDynamicTime()) return getCraftingTime(stack, Math.abs(ticks));
        return ticks;
    }

    @Override
    public ItemStack assemble(OneFluidOneItemRecipeInput input, HolderLookup.Provider registries) {
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

    @Override
    public FluidStack getResultFluidStack(int amount) {
        return new FluidStack(result, amount);
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
                                Codec.INT.fieldOf("item_amount").forGetter(MasticatingRecipe::itemAmount),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("ingredient_fluid").forGetter(MasticatingRecipe::ingredientFluid),
                                Codec.INT.fieldOf("ingredient_fluid_amount").forGetter(MasticatingRecipe::ingredientFluidAmount),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(MasticatingRecipe::result),
                                Codec.INT.fieldOf("result_fluid_amount").forGetter(MasticatingRecipe::resultAmount),
                                Codec.FLOAT.fieldOf("modifier").forGetter(MasticatingRecipe::modifier),
                                Codec.INT.fieldOf("ticks").forGetter(MasticatingRecipe::ticks)).apply(inst, MasticatingRecipe::new));

        private record Tail(Fluid result, int resultAmount, float modifier, int ticks) {}

        private static final StreamCodec<RegistryFriendlyByteBuf, Tail> TAIL_STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.FLUID), Tail::result,
                        ByteBufCodecs.VAR_INT, Tail::resultAmount,
                        ByteBufCodecs.FLOAT, Tail::modifier,
                        ByteBufCodecs.VAR_INT, Tail::ticks,
                        Tail::new);

        public static final StreamCodec<RegistryFriendlyByteBuf, MasticatingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, MasticatingRecipe::ingredient,
                        ByteBufCodecs.VAR_INT, MasticatingRecipe::itemAmount,
                        ByteBufCodecs.registry(Registries.FLUID), MasticatingRecipe::ingredientFluid,
                        ByteBufCodecs.VAR_INT, MasticatingRecipe::ingredientFluidAmount,
                        TAIL_STREAM_CODEC,
                        recipe -> new Tail(recipe.result, recipe.resultAmount, recipe.modifier, recipe.ticks),
                        (ingredient, itemAmount, ingredientFluid, ingredientFluidAmount, tail) ->
                                new MasticatingRecipe(ingredient, itemAmount, ingredientFluid, ingredientFluidAmount,
                                        tail.result, tail.resultAmount, tail.modifier, tail.ticks));
    }
}
