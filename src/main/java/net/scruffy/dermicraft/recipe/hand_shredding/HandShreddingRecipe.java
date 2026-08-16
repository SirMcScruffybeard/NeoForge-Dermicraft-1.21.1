package net.scruffy.dermicraft.recipe.hand_shredding;

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
import net.scruffy.dermicraft.recipe.ModRecipes;
import org.jetbrains.annotations.NotNull;

/**
 * Generic "hand tool + item, right-click in air" crafting -- no block/machine involved, unlike
 * every other recipe type in the mod. Covers things like Scalpel/Flint + Wool -> String, and is
 * meant to be reused for future similar tool-in-one-hand-item-in-other interactions rather than
 * spawning a new recipe type per case. See {@code HandShreddingEvent} for the trigger.
 */
public record HandShreddingRecipe(Ingredient tool, Ingredient input, ItemStack result,
                                   int toolDamage, boolean consumeTool) implements Recipe<HandShreddingRecipeInput> {

    @Override @NotNull
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(tool);
        list.add(input);
        return list;
    }

    @Override
    public boolean matches(HandShreddingRecipeInput recipeInput, Level level) {
        return tool.test(recipeInput.tool()) && input.test(recipeInput.input());
    }

    @Override
    public ItemStack assemble(HandShreddingRecipeInput recipeInput, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HAND_SHREDDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.HAND_SHREDDING_TYPE.get();
    }

    /*******************************************************************************
     * Serializer
     *******************************************************************************/
    public static class Serializer implements RecipeSerializer<HandShreddingRecipe> {

        @Override
        public MapCodec<HandShreddingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HandShreddingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<HandShreddingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst ->
                        inst.group(Ingredient.CODEC.fieldOf("tool").forGetter(HandShreddingRecipe::tool),
                                Ingredient.CODEC.fieldOf("input").forGetter(HandShreddingRecipe::input),
                                ItemStack.CODEC.fieldOf("result").forGetter(HandShreddingRecipe::result),
                                com.mojang.serialization.Codec.INT.optionalFieldOf("tool_damage", 0)
                                        .forGetter(HandShreddingRecipe::toolDamage),
                                com.mojang.serialization.Codec.BOOL.optionalFieldOf("consume_tool", false)
                                        .forGetter(HandShreddingRecipe::consumeTool)
                        ).apply(inst, HandShreddingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, HandShreddingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, HandShreddingRecipe::tool,
                        Ingredient.CONTENTS_STREAM_CODEC, HandShreddingRecipe::input,
                        ItemStack.STREAM_CODEC, HandShreddingRecipe::result,
                        ByteBufCodecs.VAR_INT, HandShreddingRecipe::toolDamage,
                        ByteBufCodecs.BOOL, HandShreddingRecipe::consumeTool,
                        HandShreddingRecipe::new);
    }
}
