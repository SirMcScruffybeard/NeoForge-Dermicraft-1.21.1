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

public record VagueMasticatingRecipe(Ingredient ingredientItem, float modifier, Fluid ingredientFluid,
                                     Fluid result) implements Recipe<OneFluidOneItemRecipeInput>, IVagueRecipe {

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

    public boolean testIngredient(ItemStack stack) {

        if (!hasNutrition(stack)) return false;

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

    @Override
    public int getCraftingAmount(ItemStack stack) {
        return getCraftingAmount(stack, modifier);
    }

    @Override
    public int getCraftingTime(ItemStack stack) {
        return getCraftingTime(stack, 50);
    }

    public FluidStack getResultFluidStack(int amount) {
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
                                        Codec.FLOAT.fieldOf("modifier").forGetter(VagueMasticatingRecipe::modifier),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("ingredient_fluid").forGetter(VagueMasticatingRecipe::ingredientFluid),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(VagueMasticatingRecipe::result))
                                .apply(inst, VagueMasticatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, VagueMasticatingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, VagueMasticatingRecipe::ingredientItem,
                        ByteBufCodecs.FLOAT, VagueMasticatingRecipe::modifier,
                        ByteBufCodecs.registry(Registries.FLUID), VagueMasticatingRecipe::ingredientFluid,
                        ByteBufCodecs.registry(Registries.FLUID), VagueMasticatingRecipe::result,
                        VagueMasticatingRecipe::new);
    }
}
