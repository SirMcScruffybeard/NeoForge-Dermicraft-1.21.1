package net.scruffy.dermicraft.recipe.mutating;

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
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import org.jetbrains.annotations.NotNull;

/**
 * A Mutator recipe: an item (consumed) plus a reagent fluid (consumed) produce a different,
 * more complex item result. Unlike the Metastasizer's pattern (which stays in the machine
 * indefinitely), the input item here is fully consumed -- this is a transformation, not a
 * duplication.
 */
public record MutatingRecipe(Ingredient ingredient, Fluid fluid, int fluidAmount, ItemStack result, int ticks)
        implements Recipe<OneFluidOneItemRecipeInput> {

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

    public boolean testIngredient(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    public boolean testFluid(FluidStack fluidStack) {
        if (!fluid.isSame(fluidStack.getFluid())) return false;
        return fluidAmount < 0 || fluidStack.getAmount() >= fluidAmount;
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public int getCraftingTime() {
        return ticks;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    @Override @NotNull
    public ItemStack assemble(OneFluidOneItemRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override @NotNull
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MUTATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MUTATING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<MutatingRecipe> {

        @Override
        public MapCodec<MutatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MutatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<MutatingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(MutatingRecipe::ingredient),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(MutatingRecipe::fluid),
                                Codec.INT.fieldOf("fluid_amount").forGetter(MutatingRecipe::fluidAmount),
                                ItemStack.CODEC.fieldOf("result").forGetter(MutatingRecipe::result),
                                Codec.INT.fieldOf("ticks").forGetter(MutatingRecipe::ticks)).apply(inst, MutatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MutatingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, MutatingRecipe::ingredient,
                        ByteBufCodecs.registry(Registries.FLUID), MutatingRecipe::fluid,
                        ByteBufCodecs.VAR_INT, MutatingRecipe::fluidAmount,
                        ItemStack.STREAM_CODEC, MutatingRecipe::result,
                        ByteBufCodecs.VAR_INT, MutatingRecipe::ticks,
                        MutatingRecipe::new);
    }
}
