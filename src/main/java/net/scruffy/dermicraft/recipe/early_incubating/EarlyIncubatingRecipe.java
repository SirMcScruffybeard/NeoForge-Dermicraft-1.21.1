package net.scruffy.dermicraft.recipe.early_incubating;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.recipe.ModRecipes;

/**
 * A Craw-driven "living construction" recipe: {@code requiredBlock} (e.g. Craw) must be the
 * block being interacted with, and its single stored bulk stack must match {@code ingredient}
 * (item + minimum count). A separate fluid injection matching {@code fluidIngredient} then
 * triggers the craft.
 *
 * <p>Deliberately distinct from {@link net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe}:
 * the holding block is never consumed or transformed -- it only gets debited the exact
 * required item count -- and the result always drops as an {@link ItemStack} next to the
 * block, whether {@code result} is a plain item or a block's {@code BlockItem}. This avoids
 * {@code EarlyImplantRecipe}'s {@code Block.byItem}/{@code level.setBlock} assumption, which
 * silently destroys the source with no output for any item-only result.
 */
public record EarlyIncubatingRecipe(Block requiredBlock, ItemStack ingredient, FluidStack fluidIngredient,
                                    ItemStack result) implements Recipe<EarlyIncubatingRecipeInput> {

    @Override
    public boolean matches(EarlyIncubatingRecipeInput input, Level level) {
        if (input.block() != this.requiredBlock) {
            return false;
        }
        ItemStack stored = input.stored();
        return ItemStack.isSameItemSameComponents(stored, this.ingredient)
                && stored.getCount() >= this.ingredient.getCount();
    }

    public boolean testFluid(FluidStack stack) {
        return FluidStack.isSameFluidSameComponents(this.fluidIngredient, stack)
                && stack.getAmount() >= this.fluidIngredient.getAmount();
    }

    @Override
    public ItemStack assemble(EarlyIncubatingRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Shapeless world interaction handler, not a grid recipe.
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EARLY_INCUBATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.EARLY_INCUBATING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<EarlyIncubatingRecipe> {
        @Override
        public MapCodec<EarlyIncubatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EarlyIncubatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<EarlyIncubatingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("required_block").forGetter(EarlyIncubatingRecipe::requiredBlock),
                                ItemStack.CODEC.fieldOf("ingredient").forGetter(EarlyIncubatingRecipe::ingredient),
                                FluidStack.CODEC.fieldOf("fluid_ingredient").forGetter(EarlyIncubatingRecipe::fluidIngredient),
                                ItemStack.CODEC.fieldOf("result").forGetter(EarlyIncubatingRecipe::result)
                        ).apply(instance, EarlyIncubatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, EarlyIncubatingRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.BLOCK), EarlyIncubatingRecipe::requiredBlock,
                ItemStack.STREAM_CODEC, EarlyIncubatingRecipe::ingredient,
                FluidStack.STREAM_CODEC, EarlyIncubatingRecipe::fluidIngredient,
                ItemStack.STREAM_CODEC, EarlyIncubatingRecipe::result,
                EarlyIncubatingRecipe::new
        );
    }
}
