package net.scruffy.dermicraft.recipe.dipping;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import org.jetbrains.annotations.NotNull;

public record DippingRecipe(Ingredient ingredient, int itemAmount, Fluid ingredientFluid, int ingredientFluidAmount,
                            Item result, int resultAmount) implements Recipe<OneFluidOneItemRecipeInput> {

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
        return ingredient.test(stack) && stack.getCount() >= itemAmount;
    }

    public boolean testFluid(FluidStack fluidStack) {
        if (!ingredientFluid.isSame(fluidStack.getFluid())) return false;
        return ingredientFluidAmount < 0 || fluidStack.getAmount() >= ingredientFluidAmount;
    }

    @Override
    public ItemStack assemble(OneFluidOneItemRecipeInput input, HolderLookup.Provider registries) {
        return new ItemStack(result, resultAmount);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(result, resultAmount);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DIPPING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.DIPPING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<DippingRecipe> {

        @Override
        public MapCodec<DippingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DippingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<DippingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(DippingRecipe::ingredient),
                                com.mojang.serialization.Codec.INT.fieldOf("item_amount").forGetter(DippingRecipe::itemAmount),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("ingredient_fluid").forGetter(DippingRecipe::ingredientFluid),
                                com.mojang.serialization.Codec.INT.fieldOf("ingredient_fluid_amount").forGetter(DippingRecipe::ingredientFluidAmount),
                                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(DippingRecipe::result),
                                com.mojang.serialization.Codec.INT.fieldOf("result_amount").forGetter(DippingRecipe::resultAmount)
                        ).apply(inst, DippingRecipe::new));

        private record Tail(Item result, int resultAmount) {}

        private static final StreamCodec<RegistryFriendlyByteBuf, Tail> TAIL_STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.ITEM), Tail::result,
                        ByteBufCodecs.VAR_INT, Tail::resultAmount,
                        Tail::new);

        public static final StreamCodec<RegistryFriendlyByteBuf, DippingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, DippingRecipe::ingredient,
                        ByteBufCodecs.VAR_INT, DippingRecipe::itemAmount,
                        ByteBufCodecs.registry(Registries.FLUID), DippingRecipe::ingredientFluid,
                        ByteBufCodecs.VAR_INT, DippingRecipe::ingredientFluidAmount,
                        TAIL_STREAM_CODEC,
                        recipe -> new Tail(recipe.result, recipe.resultAmount),
                        (ingredient, itemAmount, ingredientFluid, ingredientFluidAmount, tail) ->
                                new DippingRecipe(ingredient, itemAmount, ingredientFluid, ingredientFluidAmount,
                                        tail.result, tail.resultAmount));
    }
}
