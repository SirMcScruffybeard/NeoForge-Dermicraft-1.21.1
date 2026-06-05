package net.scruffy.dermicraft.recipe.drooling;

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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.interfaces.VagueRecipe;
import net.scruffy.dermicraft.recipe.ModRecipes;
import org.jetbrains.annotations.NotNull;

public record VagueDroolingRecipe(Ingredient ingredient, int modifier, Fluid result) implements Recipe<SingleRecipeInput>, VagueRecipe {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        if (level.isClientSide()) return false;
        return testIngredient(input.getItem(0));
    }

    public boolean testIngredient(ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override @NotNull
    public  ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
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

    public FluidStack getResultFluidStack(int amount) {
        return new FluidStack(result, amount);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override
    public int getCraftingTime(ItemStack stack) {
        return getCraftingTime(stack, 20);
    }

    public int getCraftingAmount(ItemStack stack) {

        return getCraftingAmount(stack, modifier);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.VAGUE_DROOLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.VAGUE_DROOLING_TYPE.get();
    }


    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<VagueDroolingRecipe> {

        @Override
        @NotNull
        public MapCodec<VagueDroolingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VagueDroolingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<VagueDroolingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(VagueDroolingRecipe::ingredient),
                                        Codec.INT.fieldOf("modifier").forGetter(VagueDroolingRecipe::modifier),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(VagueDroolingRecipe::result))
                                .apply(inst, VagueDroolingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, VagueDroolingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, VagueDroolingRecipe::ingredient,
                        ByteBufCodecs.VAR_INT, VagueDroolingRecipe::modifier,
                        ByteBufCodecs.registry(Registries.FLUID), VagueDroolingRecipe::result,
                        VagueDroolingRecipe::new);
    }
}
