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
import net.scruffy.dermicraft.interfaces.IVagueRecipe;
import net.scruffy.dermicraft.recipe.ModRecipes;
import org.jetbrains.annotations.NotNull;

/**
 * Drooling Crucible's own food-boost recipe -- a sibling of {@link VagueDroolingRecipe}, not a
 * reuse of it. Identical shape/formula (both implement {@link IVagueRecipe}), but registered under
 * its own {@link RecipeType}/{@link RecipeSerializer} so its lookups never collide with Cauldron's:
 * see {@link net.scruffy.dermicraft.block.entity.custom.DroolingMachineBlockEntity}'s class javadoc
 * for why two recipes matching the same ingredient can't share one type. Cauldron's own food-boost
 * ingredient list is meant to be reused verbatim here (dermicraft-machine-notes.md: "they produce
 * what they produce regardless of food... exposure to food drives their hunger more") -- only the
 * registration/lookup scope differs, not the actual recipe data.
 */
public record VagueDroolingCrucibleRecipe(Ingredient ingredient, float modifier, Fluid result) implements Recipe<SingleRecipeInput>, IVagueRecipe {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        if (level.isClientSide()) return false;
        return testIngredient(input.getItem(0));
    }

    public boolean testIngredient(ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override @NotNull
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
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
        return getCraftingTime(stack, 10);
    }

    public int getCraftingAmount(ItemStack stack) {
        return getCraftingAmount(stack, modifier);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.VAGUE_DROOLING_CRUCIBLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.VAGUE_DROOLING_CRUCIBLE_TYPE.get();
    }


    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<VagueDroolingCrucibleRecipe> {

        @Override
        @NotNull
        public MapCodec<VagueDroolingCrucibleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VagueDroolingCrucibleRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<VagueDroolingCrucibleRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("ingredient").forGetter(VagueDroolingCrucibleRecipe::ingredient),
                                        Codec.FLOAT.fieldOf("modifier").forGetter(VagueDroolingCrucibleRecipe::modifier),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(VagueDroolingCrucibleRecipe::result))
                                .apply(inst, VagueDroolingCrucibleRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, VagueDroolingCrucibleRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, VagueDroolingCrucibleRecipe::ingredient,
                        ByteBufCodecs.FLOAT, VagueDroolingCrucibleRecipe::modifier,
                        ByteBufCodecs.registry(Registries.FLUID), VagueDroolingCrucibleRecipe::result,
                        VagueDroolingCrucibleRecipe::new);
    }
}
