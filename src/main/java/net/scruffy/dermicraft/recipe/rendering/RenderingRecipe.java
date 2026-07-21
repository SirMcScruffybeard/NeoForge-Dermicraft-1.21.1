package net.scruffy.dermicraft.recipe.rendering;

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
import net.scruffy.dermicraft.recipe.FluidOnlyRecipeInput;
import net.scruffy.dermicraft.recipe.ModRecipes;
import org.jetbrains.annotations.NotNull;

/**
 * A Render Kiln recipe: a fluid alone (no item input, no pattern) is consumed to produce a
 * fixed default item -- the fluid's "natural solid form." Distinct from {@code MetastasizingRecipe},
 * which needs a non-consumed pattern item to know what to output; the Kiln's output is fixed
 * per-fluid, so there is no pattern to test.
 */
public record RenderingRecipe(Fluid fluid, int fluidAmount, ItemStack result, int ticks)
        implements Recipe<FluidOnlyRecipeInput> {

    @Override @NotNull
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public boolean matches(FluidOnlyRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        return testFluid(input.getFluid());
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
    public ItemStack assemble(FluidOnlyRecipeInput input, HolderLookup.Provider registries) {
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
        return ModRecipes.RENDERING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RENDERING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<RenderingRecipe> {

        @Override
        public MapCodec<RenderingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RenderingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<RenderingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(RenderingRecipe::fluid),
                                Codec.INT.fieldOf("fluid_amount").forGetter(RenderingRecipe::fluidAmount),
                                ItemStack.CODEC.fieldOf("result").forGetter(RenderingRecipe::result),
                                Codec.INT.fieldOf("ticks").forGetter(RenderingRecipe::ticks)).apply(inst, RenderingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RenderingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.FLUID), RenderingRecipe::fluid,
                        ByteBufCodecs.VAR_INT, RenderingRecipe::fluidAmount,
                        ItemStack.STREAM_CODEC, RenderingRecipe::result,
                        ByteBufCodecs.VAR_INT, RenderingRecipe::ticks,
                        RenderingRecipe::new);
    }
}
