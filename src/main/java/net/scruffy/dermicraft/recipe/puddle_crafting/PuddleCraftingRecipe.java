package net.scruffy.dermicraft.recipe.puddle_crafting;

import com.mojang.serialization.Codec;
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
import net.minecraft.world.level.material.Fluid;
import net.scruffy.dermicraft.recipe.ModRecipes;

import java.util.ArrayList;
import java.util.List;

public record PuddleCraftingRecipe(List<Ingredient> ingredients, Fluid puddle,
                                   Fluid resultFluid, Item resultItem, int resultItemAmount,
                                   int ticks) implements Recipe<PuddleCraftingRecipeInput> {
    @Override
    public boolean matches(PuddleCraftingRecipeInput input, Level level) {
        // 1. Fluid validation gate
        if (input.getFluid() != this.puddle) {

            return false;
        }

        if (this.ingredients.isEmpty()) {
            return input.getItems().isEmpty();
        }

        // 2. Create a fully mutable, deep copy of the floating item stacks
        List<ItemStack> inputPool = new ArrayList<>();
        for (ItemStack stack : input.getItems()) {
            if (!stack.isEmpty()) {
                inputPool.add(stack.copy());
            }
        }

        // 3. Match ingredients by subtracting quantities from the pool
        for (Ingredient ingredient : this.ingredients) {
            boolean matchedIngredient = false;

            for (ItemStack poolStack : inputPool) {
                if (!poolStack.isEmpty() && ingredient.test(poolStack)) {
                    poolStack.shrink(1); // Take exactly 1 item out of the floating stack
                    matchedIngredient = true;
                    break; // Requirement met for this specific ingredient index
                }
            }

            // If an ingredient failed to find a match with an available item count, fail early
            if (!matchedIngredient) {
                return false;
            }
        }

        // 4. Strict Count Validation: Ensure the player didn't drop extra items
        // Every single stack in our temporary pool must have been shrunk completely to 0
        return inputPool.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack assemble(PuddleCraftingRecipeInput puddleCraftingRecipeInput, HolderLookup.Provider provider) {
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
        return ModRecipes.PUDDLE_FLUID_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.PUDDLE_FLUID_CRAFTING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<PuddleCraftingRecipe> {
        @Override
        public MapCodec<PuddleCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PuddleCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<PuddleCraftingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                        Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(PuddleCraftingRecipe::ingredients),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("puddle").forGetter(PuddleCraftingRecipe::puddle),
                                        BuiltInRegistries.FLUID.byNameCodec().fieldOf("result_fluid").forGetter(PuddleCraftingRecipe::resultFluid),
                                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("result_item").forGetter(PuddleCraftingRecipe::resultItem),
                                        Codec.INT.fieldOf("item_amount").forGetter(PuddleCraftingRecipe::resultItemAmount),
                                        Codec.INT.fieldOf("ticks").forGetter(PuddleCraftingRecipe::ticks)
                                )
                                .apply(instance, PuddleCraftingRecipe::new));


        public static final StreamCodec<RegistryFriendlyByteBuf, PuddleCraftingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), PuddleCraftingRecipe::ingredients,
                        ByteBufCodecs.registry(Registries.FLUID), PuddleCraftingRecipe::puddle,
                        ByteBufCodecs.registry(Registries.FLUID), PuddleCraftingRecipe::resultFluid,
                        ByteBufCodecs.registry(Registries.ITEM), PuddleCraftingRecipe::resultItem,
                        ByteBufCodecs.VAR_INT, PuddleCraftingRecipe::resultItemAmount,
                        ByteBufCodecs.VAR_INT, PuddleCraftingRecipe::ticks,
                        PuddleCraftingRecipe::new);
    }
}
