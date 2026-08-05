package net.scruffy.dermicraft.recipe.gadget_fabricating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.recipe.ModRecipes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A Workbench gadget-fabrication recipe: an arbitrary set of items (each an exact
 * item+count requirement) and fluids (each an exact fluid+amount requirement) are
 * consumed together to produce a fixed gadget/gadget-adjacent item -- reflects gadgets'
 * higher build complexity vs. the single-fluid "printing" recipes elsewhere (e.g.
 * {@code RenderingRecipe}). Item matching does not use {@link Ingredient}/tags -- each
 * entry is a concrete item+count -- so there is no tag-based substitution.
 *
 * <p>{@code requiredTier} gates the recipe against the Workbench's own station tier (see
 * the Fabrication page plan in {@code dermicraft-gear-stations-notes.md}) -- a Workbench
 * below this tier cannot start the recipe even with every ingredient available, and its
 * Fabrication-page icon shows greyed out rather than hidden.
 */
public record GadgetFabricatingRecipe(List<ItemStack> items, List<FluidStack> fluids, ItemStack result, int ticks,
                                      int requiredTier) implements Recipe<GadgetFabricatingRecipeInput> {

    @Override @NotNull
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (ItemStack stack : items) {
            list.add(Ingredient.of(stack.getItem()));
        }
        return list;
    }

    @Override
    public boolean matches(GadgetFabricatingRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        return testItems(input.getItems()) && testFluids(input.getFluids());
    }

    public boolean testItems(List<ItemStack> available) {
        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack stack : available) {
            pool.add(stack.copy());
        }

        for (ItemStack required : items) {
            boolean matched = false;
            for (ItemStack poolStack : pool) {
                if (!poolStack.isEmpty() && ItemStack.isSameItemSameComponents(poolStack, required)
                        && poolStack.getCount() >= required.getCount()) {
                    poolStack.shrink(required.getCount());
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    public boolean testFluids(List<FluidStack> available) {
        for (FluidStack required : fluids) {
            boolean matched = false;
            for (FluidStack candidate : available) {
                if (!candidate.isEmpty() && FluidStack.isSameFluidSameComponents(candidate, required)
                        && candidate.getAmount() >= required.getAmount()) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    public int getCraftingTime() {
        return ticks;
    }

    public boolean meetsTier(int stationTier) {
        return stationTier >= requiredTier;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    @Override @NotNull
    public ItemStack assemble(GadgetFabricatingRecipeInput input, HolderLookup.Provider registries) {
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
        return ModRecipes.GADGET_FABRICATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GADGET_FABRICATING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<GadgetFabricatingRecipe> {

        @Override
        public MapCodec<GadgetFabricatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GadgetFabricatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<GadgetFabricatingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(ItemStack.CODEC.listOf().fieldOf("items").forGetter(GadgetFabricatingRecipe::items),
                                FluidStack.CODEC.listOf().fieldOf("fluids").forGetter(GadgetFabricatingRecipe::fluids),
                                ItemStack.CODEC.fieldOf("result").forGetter(GadgetFabricatingRecipe::result),
                                Codec.INT.fieldOf("ticks").forGetter(GadgetFabricatingRecipe::ticks),
                                Codec.INT.fieldOf("required_tier").forGetter(GadgetFabricatingRecipe::requiredTier))
                                .apply(inst, GadgetFabricatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, GadgetFabricatingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), GadgetFabricatingRecipe::items,
                        FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()), GadgetFabricatingRecipe::fluids,
                        ItemStack.STREAM_CODEC, GadgetFabricatingRecipe::result,
                        ByteBufCodecs.VAR_INT, GadgetFabricatingRecipe::ticks,
                        ByteBufCodecs.VAR_INT, GadgetFabricatingRecipe::requiredTier,
                        GadgetFabricatingRecipe::new);
    }
}
