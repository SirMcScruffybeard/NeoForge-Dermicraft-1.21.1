package net.scruffy.dermicraft.recipe.early_implant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.recipe.ModRecipes;

import java.util.ArrayList;
import java.util.List;

public record EarlyImplantRecipe(List<Ingredient> ingredients, Ingredient sutureTool,
                                 FluidStack fluidIngredient, Item result) implements Recipe<EarlyImplantRecipeInput> {

    // Quantity-aware subtraction checking loop
    @Override
    public boolean matches(EarlyImplantRecipeInput input, Level level) {

        if (input.getItems().size() != this.ingredients.size()) {
            return false;
        }

        List<ItemStack> inputPool = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            inputPool.add(input.getItem(i).copy());
        }

        for (Ingredient ingredient : this.ingredients) {
            boolean matchedIngredient = false;
            for (ItemStack poolStack : inputPool) {
                if (!poolStack.isEmpty() && ingredient.test(poolStack)) {
                    poolStack.shrink(1);
                    matchedIngredient = true;
                    break;
                }
            }
            if (!matchedIngredient) return false;
        }
        return inputPool.stream().allMatch(ItemStack::isEmpty);
    }

    // Helper checks to keep BlockEntity interaction logic elegant
    public boolean testSuture(ItemStack stack) {
        return this.sutureTool.test(stack);
    }

    public boolean testFluid(FluidStack stack) {
        return FluidStack.isSameFluidSameComponents(this.fluidIngredient, stack) && stack.getAmount() >= this.fluidIngredient.getAmount();
    }

    // Standard Recipe Overrides
    @Override
    public ItemStack assemble(EarlyImplantRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Shapeless world interaction handler
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(result);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EARLY_IMPLANT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        // Route this to wherever your custom recipe types are registered
        return ModRecipes.EARLY_IMPLANT_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<EarlyImplantRecipe> {
        @Override
        public MapCodec<EarlyImplantRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EarlyImplantRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<EarlyImplantRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                // Changed to standard CODEC to allow empty lists if needed
                                Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(EarlyImplantRecipe::ingredients),
                                Ingredient.CODEC.fieldOf("suture_tool").forGetter(EarlyImplantRecipe::sutureTool),
                                FluidStack.CODEC.fieldOf("fluid_ingredient").forGetter(EarlyImplantRecipe::fluidIngredient),
                                BuiltInRegistries.ITEM.byNameCodec().fieldOf("resultFluid").forGetter(EarlyImplantRecipe::result)
                        ).apply(instance, EarlyImplantRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, EarlyImplantRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), EarlyImplantRecipe::ingredients,
                Ingredient.CONTENTS_STREAM_CODEC, EarlyImplantRecipe::sutureTool,
                FluidStack.STREAM_CODEC, EarlyImplantRecipe::fluidIngredient,
                ByteBufCodecs.registry(Registries.ITEM), EarlyImplantRecipe::result,
                EarlyImplantRecipe::new
        );
    }
}
