package net.scruffy.dermicraft.recipe.effluencing;

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
import net.scruffy.dermicraft.recipe.TwoFluidRecipeInput;
import org.jetbrains.annotations.NotNull;

public record EffluencingRecipe(Fluid fluidA, int fluidAAmount, Fluid fluidB, int fluidBAmount,
                                 Fluid result, int resultAmount, int ticks)
        implements Recipe<TwoFluidRecipeInput> {

    @Override @NotNull
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public boolean matches(TwoFluidRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        boolean straight = testFluid(input.fluidA(), fluidA, fluidAAmount)
                && testFluid(input.fluidB(), fluidB, fluidBAmount);
        boolean swapped = testFluid(input.fluidA(), fluidB, fluidBAmount)
                && testFluid(input.fluidB(), fluidA, fluidAAmount);

        return straight || swapped;
    }

    private boolean testFluid(FluidStack stack, Fluid targetFluid, int targetAmount) {
        if (!targetFluid.isSame(stack.getFluid())) return false;
        return targetAmount < 0 || stack.getAmount() >= targetAmount;
    }

    // True when tankAFluid/tankBFluid line up with fluidA/fluidB in this recipe's own
    // (non-swapped) order. Callers use this once matches() has already confirmed a match, to
    // know which tank to draw which required amount from.
    public boolean isStraightOrientation(Fluid tankAFluid, Fluid tankBFluid) {
        return fluidA.isSame(tankAFluid) && fluidB.isSame(tankBFluid);
    }

    public int getCraftingAmount() {
        return resultAmount;
    }

    // A negative `ticks` flags a dynamic crafting time, scaled by the result amount rather than
    // a food item (the Effluentcer has no item ingredient to scale off) -- interpreted as
    // "ticks per 100 mB of output." Positive `ticks` is used directly as a fixed time.
    public int getCraftingTime() {
        if (ticks >= 0) return ticks;
        return Math.max(1, Math.round(Math.abs(ticks) * (resultAmount / 100f)));
    }

    public FluidStack getResultFluidStack(int amount) {
        return new FluidStack(result, amount);
    }

    @Override @NotNull
    public ItemStack assemble(TwoFluidRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override @NotNull
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EFFLUENCING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.EFFLUENCING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<EffluencingRecipe> {

        @Override
        public MapCodec<EffluencingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EffluencingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<EffluencingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid_a").forGetter(EffluencingRecipe::fluidA),
                                Codec.INT.fieldOf("fluid_a_amount").forGetter(EffluencingRecipe::fluidAAmount),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid_b").forGetter(EffluencingRecipe::fluidB),
                                Codec.INT.fieldOf("fluid_b_amount").forGetter(EffluencingRecipe::fluidBAmount),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(EffluencingRecipe::result),
                                Codec.INT.fieldOf("result_fluid_amount").forGetter(EffluencingRecipe::resultAmount),
                                Codec.INT.fieldOf("ticks").forGetter(EffluencingRecipe::ticks))
                                .apply(inst, EffluencingRecipe::new));

        private record Tail(Fluid result, int resultAmount, int ticks) {}

        private static final StreamCodec<RegistryFriendlyByteBuf, Tail> TAIL_STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.FLUID), Tail::result,
                        ByteBufCodecs.VAR_INT, Tail::resultAmount,
                        ByteBufCodecs.VAR_INT, Tail::ticks,
                        Tail::new);

        public static final StreamCodec<RegistryFriendlyByteBuf, EffluencingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.FLUID), EffluencingRecipe::fluidA,
                        ByteBufCodecs.VAR_INT, EffluencingRecipe::fluidAAmount,
                        ByteBufCodecs.registry(Registries.FLUID), EffluencingRecipe::fluidB,
                        ByteBufCodecs.VAR_INT, EffluencingRecipe::fluidBAmount,
                        TAIL_STREAM_CODEC,
                        recipe -> new Tail(recipe.result(), recipe.resultAmount(), recipe.ticks()),
                        (fluidA, fluidAAmount, fluidB, fluidBAmount, tail) ->
                                new EffluencingRecipe(fluidA, fluidAAmount, fluidB, fluidBAmount,
                                        tail.result(), tail.resultAmount(), tail.ticks()));
    }
}
