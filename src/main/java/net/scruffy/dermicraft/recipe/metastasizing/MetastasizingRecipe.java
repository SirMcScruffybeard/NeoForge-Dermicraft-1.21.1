package net.scruffy.dermicraft.recipe.metastasizing;

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
 * A Metastasizer recipe: a fluid (consumed) plus a non-consumed <b>pattern</b> item produce
 * an item result. The pattern item stays in the machine indefinitely -- only the fluid is
 * spent. The default use case duplicates the pattern (result is a copy of the pattern block),
 * but the result is stored explicitly so a recipe can output a different item entirely.
 */
public record MetastasizingRecipe(Ingredient pattern, Fluid fluid, int fluidAmount, ItemStack result, int ticks)
        implements Recipe<OneFluidOneItemRecipeInput> {

    @Override @NotNull
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(pattern);
        return list;
    }

    @Override
    public boolean matches(OneFluidOneItemRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        return testPattern(input.getItem(0))
                && testFluid(input.getFluid());
    }

    public boolean testPattern(ItemStack stack) {
        return !stack.isEmpty() && pattern.test(stack);
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
        return ModRecipes.METASTASIZING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.METASTASIZING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<MetastasizingRecipe> {

        @Override
        public MapCodec<MetastasizingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MetastasizingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<MetastasizingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("pattern").forGetter(MetastasizingRecipe::pattern),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(MetastasizingRecipe::fluid),
                                Codec.INT.fieldOf("fluid_amount").forGetter(MetastasizingRecipe::fluidAmount),
                                ItemStack.CODEC.fieldOf("result").forGetter(MetastasizingRecipe::result),
                                Codec.INT.fieldOf("ticks").forGetter(MetastasizingRecipe::ticks)).apply(inst, MetastasizingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MetastasizingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, MetastasizingRecipe::pattern,
                        ByteBufCodecs.registry(Registries.FLUID), MetastasizingRecipe::fluid,
                        ByteBufCodecs.VAR_INT, MetastasizingRecipe::fluidAmount,
                        ItemStack.STREAM_CODEC, MetastasizingRecipe::result,
                        ByteBufCodecs.VAR_INT, MetastasizingRecipe::ticks,
                        MetastasizingRecipe::new);
    }
}
